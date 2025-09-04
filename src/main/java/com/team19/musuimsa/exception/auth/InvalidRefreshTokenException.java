package com.team19.musuimsa.exception.auth;

public class InvalidRefreshTokenException extends AuthenticationException {

    public InvalidRefreshTokenException() {
        super("유효하지 않은 리프레시 토큰입니다. 다시 로그인 해주세요.");
    }
}
