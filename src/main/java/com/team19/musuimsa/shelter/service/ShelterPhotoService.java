package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.mapillary.MapillaryPhotoAgent;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.BatchReport;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ShelterPhotoService {

    private final ShelterRepository shelterRepository;
    private final MapillaryPhotoAgent mapillaryPhotoAgent;

    @Value("${mapillary.radius-m}")
    private int radiusM;

    @Value("${mapillary.batch-throttle-ms}")
    private int throttleMs;

    public ShelterPhotoService(ShelterRepository shelterRepository, MapillaryPhotoAgent mapillaryPhotoAgent) {
        this.shelterRepository = shelterRepository;
        this.mapillaryPhotoAgent = mapillaryPhotoAgent;
    }

    // Mapillary에서 가장 가까운 사진을 찾아 S3 업로드 후 photoUrl 저장
    public Optional<String> updatePhotoAndReturnUrl(Long shelterId) {
        Shelter shelter = shelterRepository.findById(shelterId).orElse(null);
        if (shelter == null) {
            return Optional.empty();
        }

        Optional<String> url = mapillaryPhotoAgent.findAndStore(
                shelter.getLatitude(),
                shelter.getLongitude(),
                radiusM,
                shelter.getShelterId()
        );

        if (url.isEmpty()) {
            return Optional.empty();
        }

        shelter.updatePhotoUrl(url.get());
        shelterRepository.save(shelter);
        return url;
    }

    public boolean updatePhoto(Long shelterId) {
        return updatePhotoAndReturnUrl(shelterId).isPresent();
    }

    // photoUrl이 비어있는 쉼터만 페이징으로 채우기
    public BatchReport updateAllMissing(int pageSize, int maxPages) {
        int processed = 0;
        int updated = 0;
        int failed = 0;

        for (int page = 0; page < maxPages; page++) {
            List<Long> ids = shelterRepository.findPendingShelterIds(PageRequest.of(0, pageSize));
            if (ids == null || ids.isEmpty()) {
                break;
            }

            for (Long id : ids) {
                processed++;
                try {
                    if (updatePhoto(id)) {
                        updated++;
                    }
                } catch (Exception e) {
                    failed++;
                }
                if (throttleMs > 0) {
                    try {
                        Thread.sleep(throttleMs);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        return new BatchReport(processed, updated, failed);
    }
}
