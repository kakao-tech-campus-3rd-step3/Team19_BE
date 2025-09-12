package com.team19.musuimsa.shelter.dto;

public record ShelterResponse(
        Long shelterId,
        String name,
        String address,
        double latitude,
        double longitude,
        String distance,
        OperatingHoursResponse operatingHoursResponse,
        int capacity,
        boolean isOutdoors,
        CoolingEquipment coolingEquipment,
        int totalRating,
        int reviewCount,
        String photoUrl
) {
    public record CoolingEquipment(
            Integer fanCount,
            Integer airConditionerCount
    ) {
    }
}
