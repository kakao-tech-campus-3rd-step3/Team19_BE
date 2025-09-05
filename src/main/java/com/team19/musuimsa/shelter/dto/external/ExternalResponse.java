package com.team19.musuimsa.shelter.dto.external;

import java.util.List;

public record ExternalResponse(
        Header header,
        Integer numOfRows,
        Integer pageNo,
        Integer totalCount,
        List<ExternalShelterItem> body
) {
    public record Header(
            String resultMsg,
            String resultCode,
            String errorMsg
    ) {
    }
}
