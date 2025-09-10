package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.exception.notfound.ShelterNotFoundException;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.ShelterResponse;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.shelter.util.ShelterDtoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelterService {

    private static final int DEFAULT_RADIUS = 1000;

    private final ShelterRepository shelterRepository;

    public List<NearbyShelterResponse> findNearbyShelters(double latitude, double longitude) {
        return shelterRepository.findNearbyShelters(latitude, longitude, DEFAULT_RADIUS)
                .stream()
                .map(ShelterDtoUtils::toNearbyDto)
                .toList();
    }

    public ShelterResponse getShelter(Long shelterId) {
        Shelter s = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        return ShelterDtoUtils.toDetailDto(s);
    }
}
