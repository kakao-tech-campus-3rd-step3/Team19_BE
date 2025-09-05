package com.team19.musuimsa.shelter.dto;

import java.util.Map;

public record NearbyShelterResponse(
        Long shelterId,
        String name,
        String address,
        double latitude,
        double longitude,
        String distance,
        boolean isOpened,
        boolean isOutdoors,
        Map<String, String> operatingHours,
        double averageRating, // 소수 1자리
        String photoUrl
) {}
