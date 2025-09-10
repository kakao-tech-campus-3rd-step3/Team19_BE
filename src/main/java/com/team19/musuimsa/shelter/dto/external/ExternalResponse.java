package com.team19.musuimsa.shelter.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalResponse(
        Header header,
        Integer numOfRows,
        Integer pageNo,
        Integer totalCount,
        List<ExternalShelterItem> body
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            String resultMsg,
            String resultCode,
            String errorMsg
    ) {
    }
}
