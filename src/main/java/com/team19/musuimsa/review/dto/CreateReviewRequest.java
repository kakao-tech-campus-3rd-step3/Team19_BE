package com.team19.musuimsa.review.dto;


public record CreateReviewRequest(
        String title,
        String content,
        int rating,
        String photoUrl
) {

}
