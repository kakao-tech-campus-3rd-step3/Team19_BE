package com.team19.musuimsa.shelter.controller;

import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.ShelterResponse;
import com.team19.musuimsa.shelter.service.ShelterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shelters")
@RequiredArgsConstructor
public class ShelterController {

    private final ShelterService shelterService;

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
    public ResponseEntity<ShelterResponse> getShelter(@PathVariable Long shelterId) {
        return ResponseEntity.ok(shelterService.getShelter(shelterId));
    }
}
