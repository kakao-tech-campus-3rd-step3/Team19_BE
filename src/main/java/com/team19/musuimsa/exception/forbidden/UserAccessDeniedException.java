package com.team19.musuimsa.exception.forbidden;

public class UserAccessDeniedException extends ForbiddenException {

    public UserAccessDeniedException() {
        super("본인의 리뷰에만 접근할 수 있습니다.");
    }
}
