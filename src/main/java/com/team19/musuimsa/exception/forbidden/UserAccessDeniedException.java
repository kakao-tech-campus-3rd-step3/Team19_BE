package com.team19.musuimsa.exception.forbidden;

import java.nio.file.AccessDeniedException;

public class UserAccessDeniedException extends AccessDeniedException {

    public UserAccessDeniedException(String message) {
        super(message);
    }
}
