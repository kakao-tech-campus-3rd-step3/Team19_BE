package com.team19.musuimsa.shelter.dto.map;

import com.team19.musuimsa.shelter.dto.OperatingHoursResponse;

public record MapShelterResponse(
        Long id,
        String name,
        String address,
        double latitude,
        double longitude,
        String distance,
        Boolean hasAircon,
        Integer capacity,
        String photoUrl,
        OperatingHoursResponse operatingHours,
        Double averageRating
) implements MapFeature {
}
