package com.team19.musuimsa.shelter.dto.map;

public record ClusterFeature(
        String id,
        double lat,
        double lng,
        int count
) implements MapFeature {
}
