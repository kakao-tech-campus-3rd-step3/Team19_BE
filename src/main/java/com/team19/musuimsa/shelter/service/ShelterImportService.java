package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.exception.external.ExternalApiException;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShelterImportService {

    private final ShelterOpenApiClient shelterOpenApiClient;

    @PersistenceContext
    private final EntityManager entityManager;


    public int importOnce() {
        int page = 1;
        int saved = 0;

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
                    Shelter shelter = toShelter(item);
                    entityManager.merge(shelter);
                    saved++;
                }
                log.info("[Shelter Import] page={} merge 완료 (누적 저장 수={})", page, saved);

                // 페이지네이션 진행 판단
                int total = res.totalCount() == null ? 0 : res.totalCount();
                int rows = res.numOfRows() == null ? 0 : res.numOfRows();
                int lastPage = (rows > 0) ? (int) Math.ceil(total / (double) rows) : page;
                log.debug("[Shelter Import] page={} 페이지네이션: total={}, rows={}, lastPage={}", page, total, rows, lastPage);

                long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
                log.info("[Shelter Import] ==== END page={} ({} ms) ====", page, elapsedMs);

                if (page >= lastPage) break;
                page++;

            } catch (ExternalApiException e) {
                log.error("[Shelter Import] API 호출 실패 (page={}): {}", page, e.getMessage(), e);
                break;
            } catch (Exception e) {
                log.error("[Shelter Import] 처리 중 예외 (page={}): {}", page, e.getMessage(), e);
                break;
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

    // 쉼터 매핑
    private Shelter toShelter(ExternalShelterItem i) {
        return Shelter.builder()
                .shelterId(i.rstrFcltyNo())
                .name(i.rstrNm())
                .address(i.rnDtlAdres())
                .latitude(i.la())
                .longitude(i.lo())
                .capacity(i.usePsblNmpr())
                .fanCount(i.colrHoldElefn())
                .airConditionerCount(i.colrHoldArcdtn())
                .weekdayOpenTime(parseTime(i.wkdayOperBeginTime()))
                .weekdayCloseTime(parseTime(i.wkdayOperEndTime()))
                .weekendOpenTime(parseTime(i.wkendHdayOperBeginTime()))
                .weekendCloseTime(parseTime(i.wkendHdayOperEndTime()))
                .isOutdoors("002".equals(i.fcltyTy()))
                .photoUrl(null)
                .build();
    }

    // String 시간 파싱
    private static LocalTime parseTime(String raw) {
        if (raw == null) {
            return null;
        }

        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }

        if (digits.length() == 3) {
            digits = "0" + digits;
        }

        return LocalTime.parse(digits, DateTimeFormatter.ofPattern("HHmm"));
    }
}