package com.team19.musuimsa.shelter.dto.map;

import java.util.List;

public record MapResponse(
        String level,
        List<MapFeature> items,
        Integer total
) {
}
