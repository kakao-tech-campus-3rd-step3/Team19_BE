package com.team19.musuimsa.mapillary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MapillaryImageResponse(
        String id,
        @JsonProperty("thumb_2048_url")
        String thumb2048Url,
        @JsonProperty("thumb_1024_url")
        String thumb1024Url,
        @JsonProperty("computed_geometry")
        MapillaryGeometryResponse computedGeometry,
        @JsonProperty("captured_at")
        Long capturedAt
) {
    public String bestThumbUrl() {
        return thumb2048Url != null ? thumb2048Url : thumb1024Url;
    }

    public Double latitude() {
        return (computedGeometry == null || computedGeometry.coordinates() == null)
                ? null : computedGeometry.coordinates()[1];
    }

    public Double longitude() {
        return (computedGeometry == null || computedGeometry.coordinates() == null)
                ? null : computedGeometry.coordinates()[0];
    }
}
