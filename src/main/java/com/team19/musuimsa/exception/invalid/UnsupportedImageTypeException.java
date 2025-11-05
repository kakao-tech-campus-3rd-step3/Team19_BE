package com.team19.musuimsa.exception.invalid;

public class UnsupportedImageTypeException extends InvalidException {
    public UnsupportedImageTypeException(String contentType) {
        super("지원하지 않는 이미지 형식입니다: " + contentType + " (허용: image/jpeg, image/png, image/webp)");
    }
}