package com.team19.musuimsa.shelter.dto;

public record BatchUpdateResponse(
        int processed,
        int updated,
        int failed
) {
}
