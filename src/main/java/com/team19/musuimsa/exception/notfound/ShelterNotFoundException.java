package com.team19.musuimsa.exception.notfound;

public class ShelterNotFoundException extends EntityNotFoundException {

    public ShelterNotFoundException(Long shelterId) {
        super("해당 ID의 쉼터를 찾을 수 없습니다: " + shelterId);
    }
}
