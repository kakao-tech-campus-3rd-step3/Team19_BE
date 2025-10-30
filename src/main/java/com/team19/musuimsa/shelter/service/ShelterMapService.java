package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.shelter.dto.map.ClusterFeature;
import com.team19.musuimsa.shelter.dto.map.MapBoundsRequest;
import com.team19.musuimsa.shelter.dto.map.MapFeature;
import com.team19.musuimsa.shelter.dto.map.MapResponse;
import com.team19.musuimsa.shelter.dto.map.MapShelterResponse;
import com.team19.musuimsa.shelter.dto.map.MapShelterRow;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.shelter.util.Clusterer;
import com.team19.musuimsa.shelter.util.GeoHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelterMapService {

    private final ShelterRepository shelterRepository;

    @Cacheable(value = "sheltersMap", key = "#root.target.cacheKey(#req)", sync = true)
    @Transactional(readOnly = true)
    public MapResponse getByBbox(MapBoundsRequest req) {
        int precision = GeoHashUtil.geohashPrecisionForZoom(req.zoom());
        Pageable pageable = PageRequest.of(req.pageOrDefault(), req.sizeOrDefault());

        BigDecimal minLat = toBigDecimal(req.minLat());
        BigDecimal minLng = toBigDecimal(req.minLng());
        BigDecimal maxLat = toBigDecimal(req.maxLat());
        BigDecimal maxLng = toBigDecimal(req.maxLng());

        double spanLat = Math.abs(req.maxLat() - req.minLat());
        double spanLng = Math.abs(req.maxLng() - req.minLng());

        int total = shelterRepository.countInBbox(minLat, minLng, maxLat, maxLng);

        // 1) cluster 레벨: 포인트만 모아서 클러스터 생성 (운영시간 X)
        if (spanLat > 3.0 || spanLng > 3.0 || req.zoom() < 13) {
            List<MapShelterResponse> points = shelterRepository.findInBbox(
                    minLat, minLng, maxLat, maxLng, pageable);
            List<ClusterFeature> clusters = Clusterer.byGeohash(points, precision);
            return new MapResponse("cluster", new ArrayList<MapFeature>(clusters), total);
        }

        // 2) summary/detail 레벨: 시간 포함 행을 받아 오늘(KST) 기준으로 운영시간과 함께 반환
        List<MapShelterRow> rows = shelterRepository.findInBboxWithHours(
                minLat, minLng, maxLat, maxLng, pageable);
        List<MapShelterResponse> items = rows.stream().map(this::toTodayResponse).toList();

        if (req.zoom() < 16) {
            return new MapResponse("summary", new ArrayList<MapFeature>(items), total);
        } else {
            return new MapResponse("detail", new ArrayList<MapFeature>(items), total);
        }
    }

    @SuppressWarnings("unused")
    public String cacheKey(MapBoundsRequest request) {
        int geohashPrecision = GeoHashUtil.geohashPrecisionForZoom(request.zoom());
        String snappedGeohash = GeoHashUtil.snapBbox(toBigDecimal(request.minLat()), toBigDecimal(request.minLng()), toBigDecimal(request.maxLat()), toBigDecimal(request.maxLng()), geohashPrecision);
        return "v1:z" + request.zoom() + ":gh:" + snappedGeohash + ":p" + request.pageOrDefault() + ":s" + request.sizeOrDefault();
    }

    private static BigDecimal toBigDecimal(double d) {
        return BigDecimal.valueOf(d);
    }

    private MapShelterResponse toTodayResponse(MapShelterRow mapShelterRow) {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        DayOfWeek dow = LocalDate.now(kst).getDayOfWeek();
        boolean weekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

        String fromTime = weekend ? mapShelterRow.weekendOpenTime() : mapShelterRow.weekdayOpenTime();
        String toTime = weekend ? mapShelterRow.weekendCloseTime() : mapShelterRow.weekdayCloseTime();

        String from = normalizeHm(fromTime);
        String to = normalizeHm(toTime);

        String hours = mergeHours(from, to);

        return new MapShelterResponse(
                mapShelterRow.id(),
                mapShelterRow.name(),
                mapShelterRow.latitude(),
                mapShelterRow.longitude(),
                mapShelterRow.hasAircon(),
                mapShelterRow.capacity(),
                mapShelterRow.photoUrl(),
                hours
        );
    }

    private String normalizeHm(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String digits = raw.replaceAll("\\D+", "");
        if (digits.length() < 3 || digits.length() > 4) {
            return raw;
        }

        if (digits.length() == 3) {
            digits = "0" + digits;
        }

        return digits.substring(0, 2) + ":" + digits.substring(2, 4);
    }

    private String mergeHours(String from, String to) {
        if (from == null && to == null) {
            return null;
        }
        if (from == null) {
            return "~" + to;
        }
        if (to == null) {
            return from + "~";
        }

        return from + "~" + to;
    }

}
