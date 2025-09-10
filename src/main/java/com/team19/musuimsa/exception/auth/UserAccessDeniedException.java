package com.team19.musuimsa.exception.auth;

public class UserAccessDeniedException extends AuthenticationException {

    public UserAccessDeniedException(String message) {
        super(message);
    }
}
