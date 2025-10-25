package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.exception.external.ExternalApiException;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.ChangedPoint;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import com.team19.musuimsa.shelter.util.GeoHashUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShelterImportService {

    private final ShelterOpenApiClient shelterOpenApiClient;

    @PersistenceContext
    private final EntityManager entityManager;

    private final CacheManager cacheManager;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String SHELTERS_CACHE = "sheltersMap";
    private static final String CACHE_NAME_PREFIX = "musuimsa::" + SHELTERS_CACHE + "::v1:z";

    public int importOnce() {
        int page = 1;
        int saved = 0;
        boolean changed = false;

        // 위치가 바뀐 쉼터 모음
        List<ChangedPoint> moved = new ArrayList<>();

        try {
            while (true) {
                long t0 = System.nanoTime();
                try {
                    log.info("[Shelter Import] ==== START page={} ====", page);

                    ExternalResponse res = shelterOpenApiClient.fetchPage(page);
                    if (res == null) {
                        log.warn("[Shelter Import] page={} 응답이 null 입니다.", page);
                        break;
                    }
                    log.debug("[Shelter Import] page={} header={}, numOfRows={}, totalCount={}",
                            page, res.header(), res.numOfRows(), res.totalCount());

                    // DTO-엔티티 매핑
                    List<ExternalShelterItem> items = safeItems(res);
                    log.info("[Shelter Import] page={} 수신 아이템 수={}", page, items.size());

                    for (ExternalShelterItem item : items) {
                        // 1) 기존 값
                        Shelter before = entityManager.find(Shelter.class, item.rstrFcltyNo());

                        // 2) 저장할 값
                        Shelter after = Shelter.toShelter(item);

                        // 3) 위치 변경 감지(pre-merge)
                        if (before != null
                                && (notEquals(before.getLatitude(), after.getLatitude())
                                || notEquals(before.getLongitude(), after.getLongitude()))) {

                            moved.add(new ChangedPoint(
                                    before.getShelterId(),
                                    before.getLatitude(), before.getLongitude(),
                                    after.getLatitude(), after.getLongitude()
                            ));
                        }

                        // 4) merge
                        entityManager.merge(after);
                        saved++;
                        changed = true;

                        // 5) 대량 처리시 메모리 안정화
                        if (saved % 500 == 0) {
                            entityManager.flush();
                            entityManager.clear();
                        }
                    }
                    log.info("[Shelter Import] page={} merge 완료 (누적 저장 수={})", page, saved);

                    // 페이지네이션 진행 판단
                    int total = res.totalCount() == null ? 0 : res.totalCount();
                    int rows = res.numOfRows() == null ? 0 : res.numOfRows();
                    int lastPage = (rows > 0) ? (int) Math.ceil(total / (double) rows) : page;
                    log.debug("[Shelter Import] page={} 페이지네이션: total={}, rows={}, lastPage={}", page,
                            total, rows, lastPage);

                    long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
                    log.info("[Shelter Import] ==== END page={} ({} ms) ====", page, elapsedMs);

                    if (page >= lastPage) {
                        break;
                    }
                    page++;

                } catch (ExternalApiException e) {
                    log.error("[Shelter Import] API 호출 실패 (page={}): {}", page, e.getMessage(), e);
                    break;
                } catch (Exception e) {
                    log.error("[Shelter Import] 처리 중 예외 (page={}): {}", page, e.getMessage(), e);
                    break;
                }
            }
        } finally {
            // 변경이 있었으면 캐시 무효화 (커밋 이후)
            if (changed) {
                invalidateCacheAfterCommit(moved);
            }
        }

        log.info("[Shelter Import] 종료. 총 저장 수={}", saved);
        return saved;
    }

    // 커밋 이후 캐시 무효화 등록
    private void invalidateCacheAfterCommit(List<ChangedPoint> moved) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doInvalidate(moved);
                }
            });
        } else {
            doInvalidate(moved);
        }
    }

    private void doInvalidate(List<ChangedPoint> moved) {
        // Redis가 없거나(=dev), 선택 무효화 대상도 없으면 전체 clear
        if (redisTemplate == null || moved == null || moved.isEmpty()) {
            Cache cache = cacheManager.getCache(SHELTERS_CACHE);
            if (cache != null) {
                cache.clear();
            }
            log.info("[Shelter Import] {} cache cleared (fallback)", SHELTERS_CACHE);
            return;
        }

        int[] zooms = {12, 13, 14, 15, 16, 17};

        RedisConnection conn = null;
        try {
            conn = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection();

            Set<String> patterns = new HashSet<>();
            for (ChangedPoint p : moved) {
                for (int z : zooms) {
                    int precision = GeoHashUtil.geohashPrecisionForZoom(z);
                    if (p.oldLat() != null && p.oldLng() != null) {
                        patterns.add(keyPattern(z, p.oldLat(), p.oldLng(), precision));
                    }
                    if (p.newLat() != null && p.newLng() != null) {
                        patterns.add(keyPattern(z, p.newLat(), p.newLng(), precision));
                    }
                }
            }

            int deleted = 0;
            for (String pattern : patterns) {
                deleted += scanAndDelete(conn, pattern);
            }

            log.info("[Shelter Import] selective invalidation done. keys_deleted={}", deleted);

        } catch (DataAccessResourceFailureException | NullPointerException ex) {
            // Redis에 문제 있으면 폴백
            Cache cache = cacheManager.getCache(SHELTERS_CACHE);
            if (cache != null) {
                cache.clear();
            }
            log.warn("[Shelter Import] selective invalidation failed. fallback to clear()", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    // pattern 으로 SCAN 후 배치 DEL
    private int scanAndDelete(RedisConnection conn, String pattern) {
        int removed = 0;

        ScanOptions opts = ScanOptions.scanOptions()
                .match(pattern)
                .count(1_000)
                .build();

        try (Cursor<byte[]> cur = conn.keyCommands().scan(opts)) {
            List<byte[]> batch = new ArrayList<>(500);
            while (cur.hasNext()) {
                byte[] k = cur.next();
                batch.add(k);

                if (batch.size() >= 500) {
                    conn.keyCommands().del(batch.toArray(new byte[0][]));
                    removed += batch.size();
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                conn.keyCommands().del(batch.toArray(new byte[0][]));
                removed += batch.size();
            }
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("SCAN/DEL failed: " + pattern, e);
        }
        return removed;
    }


    // 줌/위치/정밀도로 Redis 키 패턴 생성
    private static String keyPattern(int z, BigDecimal lat, BigDecimal lng, int precision) {
        // GeoHashUtil.snapBbox(BigDecimal, BigDecimal, BigDecimal, BigDecimal, int) 사용
        String cell = GeoHashUtil.snapBbox(lat, lng, lat, lng, precision);
        return CACHE_NAME_PREFIX + z + ":gh:" + cell + ":*";
    }

    private static boolean notEquals(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a != b;
        }
        return a.compareTo(b) != 0;
    }

    // 응답 body null-safe 추출
    private static List<ExternalShelterItem> safeItems(ExternalResponse res) {
        if (res == null || res.body() == null) {
            return List.of();
        }
        return res.body();
    }
}