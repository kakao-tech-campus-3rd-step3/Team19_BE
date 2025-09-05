package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.exception.external.ExternalApiException;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.external.*;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShelterImportService {

    private final ShelterOpenApiClient client;
    private final ShelterRepository shelterRepository;

    // 저장 없이 특정 페이지만 변환해서 반환(정상 응답/매핑 확인용)
    public List<Shelter> previewPage(int pageNo) {
        ExternalResponse res = client.fetchPage(pageNo);
        List<ExternalShelterItem> items = safeItems(res);
        List<Shelter> mapped = new ArrayList<>();
        for (ExternalShelterItem i : items) {
            toShelter(i).ifPresent(mapped::add);
        }
        return mapped;
    }

    // 전체 임포트 1회 (업서트)
    public int importOnce() {
        int page = 1, saved = 0;
        while (true) {
            List<Shelter> batch = new ArrayList<>();
            try {
                ExternalResponse res = client.fetchPage(page);
                List<ExternalShelterItem> items = safeItems(res);
                if (items.isEmpty()) break;

                for (ExternalShelterItem i : items) toShelter(i).ifPresent(batch::add);
                shelterRepository.saveAll(batch);
                saved += batch.size();

                ExternalPageBody body = res.body();
                int total = Optional.ofNullable(body.totalCount()).orElse(0);
                int lastPage = (int) Math.ceil(total / (double) client.pageSize());
                log.info("[Shelter Import] page {}/{} saved {}", page, lastPage, batch.size());
                if (page >= lastPage) {
                    break;
                }
                page++;

            } catch (ExternalApiException e) {
                log.error("[Shelter Import] API 호출 실패 (page={}): {}", page, e.getMessage());
                break;
            }
        }
        return saved;
    }

    private static List<ExternalShelterItem> safeItems(ExternalResponse res) {
        if (res == null || res.body() == null || res.body().items() == null) return List.of();
        return res.body().items();
    }

    private Optional<Shelter> toShelter(ExternalShelterItem i) {
        try {
            Long id = parseLong(i.RSTR_FCLTY_NO());
            if (id == null) {
                return Optional.empty();
            }

            BigDecimal la = parseBigDecimal(i.LA());
            BigDecimal lo = parseBigDecimal(i.LO());
            if (!validCoord(la, lo)) {
                return Optional.empty();
            }

            return Optional.of(Shelter.builder()
                    .shelterId(id)
                    .name(nz(i.RSTR_NM(), "무더위 쉼터"))
                    .address(nz(i.RN_DTL_ADRES(), ""))
                    .latitude(la)
                    .longitude(lo)
                    .capacity(parseInt(i.USE_PSBL_NMPR()))
                    .fanCount(parseInt(i.COLR_HOLD_ELFN()))
                    .airConditionerCount(parseInt(i.COLR_HOLD_ARCDTN()))
                    .weekdayOpenTime(parseTime(i.WKDAY_OPER_BEGIN_TIME()))
                    .weekdayCloseTime(parseTime(i.WKDAY_OPER_END_TIME()))
                    .weekendOpenTime(parseTime(i.WKEND_HDAY_OPER_BEGIN_TIME()))
                    .weekendCloseTime(parseTime(i.WKEND_HDAY_OPER_END_TIME()))
                    .isOutdoors("002".equalsIgnoreCase(nz(i.FCLTY_TY(), "")))
                    .photoUrl(nz(i.PHOTO_URL(), null))
                    .totalRating(0)
                    .reviewCount(0)
                    .build());
        } catch (Exception e) {
            log.warn("Skip invalid row: {}", i, e);
            return Optional.empty();
        }
    }

    private static boolean validCoord(BigDecimal la, BigDecimal lo) {
        if (la == null || lo == null)
            return false;

        if (la.compareTo(new BigDecimal("-90")) < 0 || la.compareTo(new BigDecimal("90")) > 0)
            return false;

        if (lo.compareTo(new BigDecimal("-180")) < 0 || lo.compareTo(new BigDecimal("180")) > 0)
            return false;

        if (la.compareTo(BigDecimal.ZERO) == 0 && lo.compareTo(BigDecimal.ZERO) == 0)
            return false;

        return true;
    }

    private static String nz(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private static BigDecimal parseBigDecimal(String s) {
        try { return (s == null || s.isBlank()) ? null : new BigDecimal(s.trim()); }
        catch (Exception e) { return null; }
    }


    private static Integer parseInt(String s) {
        try {
            return (s==null||s.isBlank())? null : Integer.parseInt(s.trim());
        } catch(Exception e){
            return null;
        }
    }

    private static Long parseLong(String s) {
        try {
            return (s==null||s.isBlank())? null : Long.parseLong(s.trim());
        } catch(Exception e){
            return null;
        }
    }

    // "0930", "9:30", "09:30:00" 등 파싱
    private static LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String v = raw.replaceAll("[^0-9]", "");
        try {
            if (v.length() == 4) {
                return LocalTime.parse(v, DateTimeFormatter.ofPattern("HHmm"));
            }
            if (v.length() == 6) {
                return LocalTime.parse(v, DateTimeFormatter.ofPattern("HHmmss"));
            }
            return LocalTime.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
