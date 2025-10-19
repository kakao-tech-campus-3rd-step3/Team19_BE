package com.team19.musuimsa.shelter.dto;

public record NearbyShelterResponse(
        Long shelterId,
        String name,
        String address,
        double latitude,
        double longitude,
        String distance,
        boolean isOutdoors,
        OperatingHoursResponse operatingHours,
        double averageRating,
        String photoUrl
) {
}
