package com.team19.musuimsa.exception.auth;

public class InvalidPasswordException extends AuthenticationException {

    public InvalidPasswordException() {
        super("현재 비밀번호가 일치하지 않습니다.");
    }
}
