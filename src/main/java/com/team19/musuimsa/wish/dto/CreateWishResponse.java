package com.team19.musuimsa.wish.dto;

import java.time.LocalDateTime;

public record CreateWishResponse(
        Long wishId,
        Long userId,
        Long shelterId,
        LocalDateTime createdAt
) {
}
