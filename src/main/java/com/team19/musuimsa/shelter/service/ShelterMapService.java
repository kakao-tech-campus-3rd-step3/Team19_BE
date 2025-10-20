package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.shelter.dto.map.ClusterFeature;
import com.team19.musuimsa.shelter.dto.map.MapBoundsRequest;
import com.team19.musuimsa.shelter.dto.map.MapFeature;
import com.team19.musuimsa.shelter.dto.map.MapResponse;
import com.team19.musuimsa.shelter.dto.map.MapShelterResponse;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelterMapService {

    private final ShelterRepository shelterRepository;

    @Cacheable(value = "sheltersMap", key = "#root.target.cacheKey(#req)", sync = true)
    @Transactional(readOnly = true)
    public MapResponse getByBbox(MapBoundsRequest req) {
        int precision = GeoHashUtil.prefixForZoom(req.zoom());
        Pageable pageable = PageRequest.of(req.pageOrDefault(), req.sizeOrDefault());

        BigDecimal minLat = toBigDecimal(req.minLat());
        BigDecimal minLng = toBigDecimal(req.minLng());
        BigDecimal maxLat = toBigDecimal(req.maxLat());
        BigDecimal maxLng = toBigDecimal(req.maxLng());

        double spanLat = Math.abs(req.maxLat() - req.minLat());
        double spanLng = Math.abs(req.maxLng() - req.minLng());

        if (spanLat > 3.0 || spanLng > 3.0 || req.zoom() < 13) {
            List<MapShelterResponse> points = shelterRepository.findInBbox(
                    minLat, minLng, maxLat, maxLng, pageable);
            List<ClusterFeature> clusters = Clusterer.byGeohash(points, precision);
            return new MapResponse("cluster", new ArrayList<MapFeature>(clusters), points.size());
        } else if (req.zoom() < 16) {
            List<MapShelterResponse> items = shelterRepository.findInBbox(
                    minLat, minLng, maxLat, maxLng, pageable);
            int total = shelterRepository.countInBbox(minLat, minLng, maxLat, maxLng);
            return new MapResponse("summary", new ArrayList<MapFeature>(items), total);
        } else {
            // TODO: 나중에 상세 내용 분리 예정 (쿼리 이용)
            List<MapShelterResponse> items = shelterRepository.findInBbox(
                    minLat, minLng, maxLat, maxLng, pageable);
            int total = shelterRepository.countInBbox(minLat, minLng, maxLat, maxLng);
            return new MapResponse("detail", new ArrayList<MapFeature>(items), total);
        }
    }

    @SuppressWarnings("unused")
    public String cacheKey(MapBoundsRequest r) {
        int precision = GeoHashUtil.prefixForZoom(r.zoom());
        String gh = GeoHashUtil.snapBbox(r.minLat(), r.minLng(), r.maxLat(), r.maxLng(), precision);
        return "v1:z" + r.zoom() + ":gh:" + gh + ":p" + r.pageOrDefault() + ":s" + r.sizeOrDefault();
    }

    private static BigDecimal toBigDecimal(double d) {
        return BigDecimal.valueOf(d);
    }
}
