package com.team19.musuimsa.shelter.controller;

import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.ShelterResponse;
import com.team19.musuimsa.shelter.dto.map.MapBoundsRequest;
import com.team19.musuimsa.shelter.dto.map.MapResponse;
import com.team19.musuimsa.shelter.service.ShelterMapService;
import com.team19.musuimsa.shelter.service.ShelterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shelters")
@RequiredArgsConstructor
public class ShelterController {

    private final ShelterService shelterService;

    private final ShelterMapService shelterMapService;

    // 쉼터 메인 페이지 바운딩박스 기반 조회
    @GetMapping
    public ResponseEntity<MapResponse> getByBbox(
            @RequestParam double minLat,
            @RequestParam double minLng,
            @RequestParam double maxLat,
            @RequestParam double maxLng,
            @RequestParam int zoom,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(shelterMapService.getByBbox(new MapBoundsRequest(minLat, minLng, maxLat, maxLng, zoom, page, size)));
    }

    // 가까운 쉼터 조회
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyShelterResponse>> findNearbyShelters(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return ResponseEntity.ok(shelterService.findNearbyShelters(latitude, longitude));
    }

    // 쉼터 상세 조회
    @GetMapping("/{shelterId}")
    public ResponseEntity<ShelterResponse> getShelter(
            @PathVariable Long shelterId,
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return ResponseEntity.ok(shelterService.getShelter(shelterId, latitude, longitude));
    }
}
