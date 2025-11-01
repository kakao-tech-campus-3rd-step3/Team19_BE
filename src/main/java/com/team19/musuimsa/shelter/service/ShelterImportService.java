package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.ChangedPoint;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import com.team19.musuimsa.shelter.util.GeoHashUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShelterImportService {

    private final ShelterOpenApiClient shelterOpenApiClient;

    private final EntityManager entityManager;

    private final CacheManager cacheManager;

    private final Optional<StringRedisTemplate> redisTemplate;

    private static final String SHELTERS_CACHE = "sheltersMap";
    private static final String CACHE_NAME_PREFIX = "musuimsa::" + SHELTERS_CACHE + "::v1:z";

    public int importOnce() {
        int page = 1;
        int saved = 0;
        boolean changed = false;

        // 위치가 바뀐 쉼터 모음
        List<ChangedPoint> moved = new ArrayList<>();

        boolean success = false;
        try {
            while (true) {
                long t0 = System.nanoTime();
                log.info("[Shelter Import] ==== START page={} ====", page);

                ExternalResponse res = shelterOpenApiClient.fetchPage(page);
                if (res == null) {
                    log.warn("[Shelter Import] page={} 응답이 null 입니다.", page);
                    break;
                }

                List<ExternalShelterItem> items = safeItems(res);
                log.info("[Shelter Import] page={} 수신 아이템 수={}", page, items.size());

                for (ExternalShelterItem item : items) {
                    // 1) 기존 값
                    Shelter before = entityManager.find(Shelter.class, item.rstrFcltyNo());

                    if (before == null) {
                        // 2) 저장할 값
                        Shelter after = Shelter.toShelter(item);
                        entityManager.persist(after);
                        saved++;
                        changed = true;
                    } else {
                        BigDecimal oldLat = before.getLatitude();
                        BigDecimal oldLng = before.getLongitude();

                        boolean anyChanged = before.updateFrom(item);
                        if (anyChanged) {
                            saved++;
                            changed = true;
                            if (notEquals(oldLat, before.getLatitude()) || notEquals(oldLng, before.getLongitude())) {
                                moved.add(
                                        new ChangedPoint(
                                                before.getShelterId(),
                                                oldLat,
                                                oldLng,
                                                before.getLatitude(),
                                                before.getLongitude()
                                        )
                                );
                            }
                        }
                    }

                    if (saved % 500 == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                }

                // 페이지네이션 진행 판단
                int total = res.totalCount() == null ? 0 : res.totalCount();
                int rows = res.numOfRows() == null ? 0 : res.numOfRows();
                int lastPage = (rows > 0) ? (int) Math.ceil(total / (double) rows) : page;
                log.info("[Shelter Import] ==== END page={} ({} ms) ====", page, (System.nanoTime() - t0) / 1_000_000);

                if (page >= lastPage) {
                    break;
                }
                page++;
            }
            success = true; // 예외 없이 끝난 경우
        } finally {
            if (success) {
                try {
                    entityManager.flush();
                } catch (Exception ignore) {
                }

                if (changed) {
                    invalidateCacheAfterCommit(moved);
                }
            }
        }

        log.info("[Shelter Import] 정상 종료. 총 저장/갱신 수={}", saved);
        return saved;
    }

    // 커밋 이후 캐시 무효화 등록
    private void invalidateCacheAfterCommit(List<ChangedPoint> moved) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("[Shelter Import] no active tx -> skip cache invalidation");
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                doInvalidate(moved);
            }
        });
    }

    private void doInvalidate(List<ChangedPoint> moved) {
        // Redis가 없거나(=dev), 선택 무효화 대상도 없으면 전체 clear
        if (redisTemplate.isEmpty() || moved == null || moved.isEmpty()) {
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
            conn = Objects.requireNonNull(redisTemplate.get().getConnectionFactory()).getConnection();

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