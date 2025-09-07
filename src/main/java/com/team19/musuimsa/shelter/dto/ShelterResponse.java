package com.team19.musuimsa.shelter.dto;

public record ShelterResponse(
        Long shelterId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        OperatingHoursResponse operatingHoursResponse,
        Integer capacity,
        Boolean isOutdoors,
        CoolingEquipment coolingEquipment,
        Integer totalRating,
        Integer reviewCount,
        String photoUrl
) {
    public record CoolingEquipment(
            Integer fanCount,
            Integer airConditionerCount
    ) {
    }
}
