package com.team19.musuimsa.shelter.dto.map;

public record MapBoundsRequest(
        double minLat,
        double minLng,
        double maxLat,
        double maxLng,
        int zoom,
        Integer page, Integer size
) {
    public int pageOrDefault() {
        return page == null ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null ? 200 : Math.min(size, 500);
    }
}
