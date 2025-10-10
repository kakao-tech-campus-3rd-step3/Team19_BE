package com.team19.musuimsa.wish.dto;

public record WishListItemResponse(
        Long shelterId,
        String name,
        String address,
        String operatingHours,
        Double averageRating,
        String photoUrl,
        String distance
) {
}
