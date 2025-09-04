package com.team19.musuimsa.review.dto;

public record UpdateReviewRequest(
        String title,
        String content,
        int rating,
        String photoUrl
) {

}
