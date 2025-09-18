package com.team19.musuimsa.exception.forbidden;

public class ReviewAccessDeniedException extends ForbiddenException {

    public ReviewAccessDeniedException() {
        super("본인의 리뷰에만 접근할 수 있습니다.");
    }
}
