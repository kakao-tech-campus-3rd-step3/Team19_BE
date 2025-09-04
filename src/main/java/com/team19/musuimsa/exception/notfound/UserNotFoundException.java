package com.team19.musuimsa.exception.notfound;

public class UserNotFoundException extends EntityNotFoundException {

    public UserNotFoundException(Long userId) {
        super("해당 ID의 사용자를 찾을 수 없습니다: " + userId);
    }
}
