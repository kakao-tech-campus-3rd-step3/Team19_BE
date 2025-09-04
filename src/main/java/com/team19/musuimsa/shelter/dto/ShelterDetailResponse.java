package com.team19.musuimsa.shelter.dto;

import java.util.Map;

public record ShelterDetailResponse(
        Long shelterId,
        String name,
        String address,
        double latitude,
        double longitude,
        Map<String, String> operatingHours,
        Integer capacity,
        boolean isOpened,
        boolean isOutdoors,
        CoolingEquipment coolingEquipment,
        Integer totalRating,
        Integer reviewCount,
        String photoUrl
) {
    public record CoolingEquipment(
            Integer fanCount,
            Integer acCount
    ) {}
}
