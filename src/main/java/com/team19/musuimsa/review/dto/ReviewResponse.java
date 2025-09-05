package com.team19.musuimsa.review.dto;

import java.time.LocalDateTime;

public record ReviewResponse(

        Long reviewId,

        Long shelterId,

        Long userId,

        String nickname,

        String title,

        String content,

        int rating,

        String photoUrl,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

}
