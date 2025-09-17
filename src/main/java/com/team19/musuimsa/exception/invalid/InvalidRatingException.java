package com.team19.musuimsa.exception.invalid;

public class InvalidRatingException extends InvalidException {

    public InvalidRatingException() {
        super("별점은 1~5점 사이만 가능합니다.");
    }

}
