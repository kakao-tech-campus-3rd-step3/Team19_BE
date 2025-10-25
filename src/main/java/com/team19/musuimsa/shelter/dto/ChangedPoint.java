package com.team19.musuimsa.shelter.dto;

public record ChangedPoint(
        Long id,
        java.math.BigDecimal oldLat,
        java.math.BigDecimal oldLng,
        java.math.BigDecimal newLat,
        java.math.BigDecimal newLng
) {
}