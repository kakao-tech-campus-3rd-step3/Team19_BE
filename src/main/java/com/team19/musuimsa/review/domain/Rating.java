package com.team19.musuimsa.review.domain;

import com.team19.musuimsa.exception.invalid.InvalidRatingException;

public final class Rating {

    private final int value;

    private Rating(int value) {
        if (value < 1 || value > 5) {
            throw new InvalidRatingException();
        }
        this.value = value;
    }

    public static Rating of(int value) {
        return new Rating(value);
    }

    public int value() {
        return value;
    }
}