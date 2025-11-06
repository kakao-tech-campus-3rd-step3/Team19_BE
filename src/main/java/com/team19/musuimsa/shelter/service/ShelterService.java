package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.exception.notfound.ShelterNotFoundException;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.ShelterResponse;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.shelter.util.ShelterDtoUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShelterService {

    private static final int DEFAULT_RADIUS = 1000;
    private static final int THREE_KM_RADIUS = 3000;

    private final ShelterRepository shelterRepository;

    public List<NearbyShelterResponse> findNearbyShelters(double latitude, double longitude) {
        List<Shelter> shelters = shelterRepository.findNearbyShelters(latitude, longitude,
                DEFAULT_RADIUS);

        return shelters.stream()
                .map(s -> ShelterDtoUtils.toNearbyDto(
                        s,
                        ShelterDtoUtils.distanceFrom(latitude, longitude, s)
                ))
                .toList();
    }

    // 3km 내 쉼터 조회
    public List<NearbyShelterResponse> getAllShelters(double latitude, double longitude) {
        List<Shelter> shelters = shelterRepository.findNearbyShelters(latitude, longitude,
                THREE_KM_RADIUS);

        return shelters.stream()
                .map(s -> ShelterDtoUtils.toNearbyDto(
                        s,
                        null
                ))
                .toList();
    }

    public ShelterResponse getShelter(Long shelterId, double latitude, double longitude) {
        Shelter shelter = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        String distance = ShelterDtoUtils.distanceFrom(latitude, longitude, shelter);

        return ShelterDtoUtils.toDetailDto(shelter, distance);
    }
}
