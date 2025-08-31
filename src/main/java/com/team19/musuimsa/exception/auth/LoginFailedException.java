package com.team19.musuimsa.exception.auth;

public class LoginFailedException extends AuthenticationException {

    public LoginFailedException() {
        super("이메일 또는 비밀번호가 일치하지 않습니다. ");
    }
}
