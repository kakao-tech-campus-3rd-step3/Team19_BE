package com.team19.musuimsa.shelter.controller;

import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.ShelterDetailResponse;
import com.team19.musuimsa.shelter.service.ShelterService;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shelters")
@RequiredArgsConstructor
@Validated
public class ShelterController {

    private final ShelterService shelterService;

    // 가까운 쉼터 조회 (Nearby): default 3000 (3km 이내)
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyShelterResponse>> nearby(
            @RequestParam @NotNull @DecimalMin(value = "-90.0") @DecimalMax("90.0") Double latitude,
            @RequestParam @NotNull @DecimalMin(value = "-180.0") @DecimalMax("180.0") Double longitude,
            @RequestParam(required = false, defaultValue = "3000") @Positive Double radius
    ) {
        return ResponseEntity.ok(shelterService.findNearby(latitude, longitude, radius));
    }

    // 쉼터 상세 조회
    @GetMapping("/{shelterId}")
    public ResponseEntity<ShelterDetailResponse> detail(@PathVariable Long shelterId) {
        return ResponseEntity.ok(shelterService.getShelter(shelterId));
    }
}
