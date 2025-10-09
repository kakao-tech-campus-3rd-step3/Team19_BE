package com.team19.musuimsa.user.dto;

import java.math.BigDecimal;

public record UserLocationUpdateRequest(
        BigDecimal latitude,
        BigDecimal longitude
) {

}