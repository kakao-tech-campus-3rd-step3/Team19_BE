package com.team19.musuimsa.shelter.dto.map;

public record MapShelterRow(
        Long id,
        String name,
        String address,
        double latitude,
        double longitude,
        Boolean hasAircon,
        Integer capacity,
        String photoUrl,
        String weekdayOpenTime,
        String weekdayCloseTime,
        String weekendOpenTime,
        String weekendCloseTime,
        Long totalRating,
        Long reviewCount
) {
}