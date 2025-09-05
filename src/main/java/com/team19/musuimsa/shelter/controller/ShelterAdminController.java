package com.team19.musuimsa.shelter.controller;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.service.ShelterImportService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shelters")
@RequiredArgsConstructor
@Validated
public class ShelterAdminController {

    private final ShelterImportService importService;

    // 외부 응답/매핑만 확인(저장 X)
    @GetMapping("/preview")
    public ResponseEntity<List<Shelter>> preview(@RequestParam(defaultValue = "1") @Positive int page) {
        return ResponseEntity.ok(importService.previewPage(page));
    }

    // 전체 페이지 1회 업서트
    @PostMapping("/import-once")
    public ResponseEntity<String> importOnce() {
        int count = importService.importOnce();
        return ResponseEntity.ok("Upserted shelters = " + count);
    }
}
