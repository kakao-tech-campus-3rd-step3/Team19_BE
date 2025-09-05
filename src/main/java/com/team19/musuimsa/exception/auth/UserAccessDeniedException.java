package com.team19.musuimsa.exception.auth;

public class UserAccessDeniedException extends AuthenticationException {

    public UserAccessDeniedException() {
        super("본인의 리뷰만 수정, 삭제할 수 있습니다.");
    }
}
