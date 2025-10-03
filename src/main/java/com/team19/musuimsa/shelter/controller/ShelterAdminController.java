package com.team19.musuimsa.shelter.controller;

import com.team19.musuimsa.shelter.dto.BatchReport;
import com.team19.musuimsa.shelter.dto.BatchUpdateResponse;
import com.team19.musuimsa.shelter.dto.ShelterImportResponse;
import com.team19.musuimsa.shelter.dto.ShelterPhotoUrlUpdateResponse;
import com.team19.musuimsa.shelter.service.ShelterImportService;
import com.team19.musuimsa.shelter.service.ShelterPhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin/shelters")
@RequiredArgsConstructor
public class ShelterAdminController {

    private final ShelterImportService importService;
    private final ShelterPhotoService photoService;

    @PostMapping("/import")
    public ResponseEntity<ShelterImportResponse> importShelters() {
        int saved = importService.importOnce();
        return ResponseEntity.ok(new ShelterImportResponse(saved));
    }


    // Shelter photoUrl 단건 저장
    @PostMapping("/photos/{shelterId}")
    public ResponseEntity<ShelterPhotoUrlUpdateResponse> updateOne(@PathVariable Long shelterId) {
        Optional<String> urlOpt = photoService.updatePhotoAndReturnUrl(shelterId);

        if (urlOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(new ShelterPhotoUrlUpdateResponse(true, urlOpt.get()));
    }

    // Shelter photoUrl 모두 저장
    @PostMapping("/photos/all")
    public ResponseEntity<BatchUpdateResponse> updateBatch(
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(defaultValue = "100") int maxPages
    ) {
        BatchReport batchReport = photoService.updateAllMissing(pageSize, maxPages);
        return ResponseEntity.ok(
                new BatchUpdateResponse(
                        batchReport.processed(),
                        batchReport.updated(),
                        batchReport.failed()
                )
        );
    }
}
