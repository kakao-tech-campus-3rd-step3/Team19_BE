package com.team19.musuimsa.exception.forbidden;

import org.springframework.security.access.AccessDeniedException;

public class UserAccessDeniedException extends AccessDeniedException {

    public UserAccessDeniedException(String message) {
        super(message);
    }
}
