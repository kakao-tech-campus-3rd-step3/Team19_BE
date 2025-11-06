package com.team19.musuimsa.shelter.dto.map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

public record MapResponse(
        String level,
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
                property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = ClusterFeature.class, name = "cluster"),
                @JsonSubTypes.Type(value = MapShelterResponse.class, name = "shelter")
        })
        List<MapFeature> items,
        Integer total
) {

}
