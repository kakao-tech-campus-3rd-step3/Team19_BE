package com.team19.musuimsa.shelter.dto.map;

public record ClusterFeature(
        String id,
        double latitude,
        double longitude,
        int count
) implements MapFeature {
}
