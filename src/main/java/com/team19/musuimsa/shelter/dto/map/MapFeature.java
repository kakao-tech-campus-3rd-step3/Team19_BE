package com.team19.musuimsa.shelter.dto.map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClusterFeature.class, name = "cluster"),
        @JsonSubTypes.Type(value = MapShelterResponse.class, name = "shelter")
})
public sealed interface MapFeature permits ClusterFeature, MapShelterResponse {

}
