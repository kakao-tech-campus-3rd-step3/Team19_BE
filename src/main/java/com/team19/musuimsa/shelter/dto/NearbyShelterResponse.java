package com.team19.musuimsa.shelter.dto;

public record NearbyShelterResponse(
        Long shelterId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        Boolean isOutdoors,
        OperatingHoursResponse operatingHoursResponse,
        Double averageRating,
        String photoUrl
) {
}
