package com.team19.musuimsa.wish.dto;

import java.util.List;

public record WishListResponse(
        List<WishListItemResponse> items
) {
}
