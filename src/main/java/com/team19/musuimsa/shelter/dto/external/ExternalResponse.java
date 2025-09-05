package com.team19.musuimsa.shelter.dto.external;

import java.util.List;

public record ExternalResponse(
        Header header,
        Body body
) {
    public record Header(
            String resultMsg,
            String resultCode,
            String errorMsg
    ) {
    }

    public record Body(
            Integer pageNo,
            Integer numOfRows,
            Integer totalCount,
            List<ExternalShelterItem> items
    ) {
    }
}
