package com.team19.musuimsa.shelter.dto.map;

public record MapShelterResponse(
        Long id,
        String name,
        double lat,
        double lng,
        Boolean hasAircon,
        Integer capacity,
        String photoUrl,
        String openFrom,
        String openTo
) implements MapFeature {
}
