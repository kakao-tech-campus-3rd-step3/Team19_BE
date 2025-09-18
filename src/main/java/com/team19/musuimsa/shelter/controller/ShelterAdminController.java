package com.team19.musuimsa.shelter.controller;

import com.team19.musuimsa.shelter.service.ShelterImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/shelters")
@RequiredArgsConstructor
public class ShelterAdminController {

    private final ShelterImportService importService;

    @PostMapping("/import")
    public ResponseEntity<Integer> importShelters() {
        int saved = importService.importOnce();
        return ResponseEntity.ok(saved);
    }
}
