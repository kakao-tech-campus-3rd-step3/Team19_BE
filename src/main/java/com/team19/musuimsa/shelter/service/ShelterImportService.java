package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.exception.external.ExternalApiException;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShelterImportService {

    private final ShelterOpenApiClient shelterOpenApiClient;

    @PersistenceContext
    private final EntityManager entityManager;

    private final CacheManager cacheManager;


    public int importOnce() {
        int page = 1;
        int saved = 0;
        boolean changed = false;

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
                        Shelter shelter = Shelter.toShelter(item);
                        entityManager.merge(shelter);
                        saved++;
                        changed = true;

                        // 대량 처리시 메모리 안정화
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
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    Cache cache = cacheManager.getCache("sheltersMap");
                                    if (cache != null) {
                                        cache.clear();
                                    }
                                    log.info("[Shelter Import] sheltersMap cache cleared after commit");
                                }
                            }
                    );
                } else {
                    Cache cache = cacheManager.getCache("sheltersMap");
                    if (cache != null) {
                        cache.clear();
                    }
                    log.info("[Shelter Import] sheltersMap cache cleared (no active tx)");
                }
            }
        }

        log.info("[Shelter Import] 종료. 총 저장 수={}", saved);
        return saved;
    }

    // 응답 body null-safe 추출
    private static List<ExternalShelterItem> safeItems(ExternalResponse res) {
        if (res == null || res.body() == null) {
            return List.of();
        }
        return res.body();
    }
}