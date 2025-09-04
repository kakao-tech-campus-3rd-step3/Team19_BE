package com.team19.musuimsa.shelter.dto.external;

import java.util.List;

public record ExternalPageBody(
        Integer pageNo,
        Integer numOfRows,
        Integer totalCount,
        List<ExternalShelterItem> items
) {}
