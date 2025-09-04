package com.team19.musuimsa.exception.notfound;

public class ReviewNotFoundException extends EntityNotFoundException {

    public ReviewNotFoundException(Long reviewId) {
        super("해당 ID의 리뷰를 찾을 수 없습니다: " + reviewId);
    }
}
