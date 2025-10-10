package com.team19.musuimsa.exception.auth;

public class AuthenticationRequiredException extends AuthenticationException {
    public AuthenticationRequiredException() {
        super("회원가입 또는 로그인 후 이용 가능합니다. ");
    }
}
