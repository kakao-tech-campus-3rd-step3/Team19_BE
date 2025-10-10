package com.team19.musuimsa.weather.dto;

import java.util.List;

public record KmaResponse(
        Response response
) {

    public record Response(
            Header header,
            Body body
    ) {

    }

    public record Header(
            String resultCode,
            String resultMsg
    ) {

    }

    public record Body(
            Items items
    ) {

    }

    public record Items(
            List<Item> item
    ) {

    }

    public record Item(
            String category,
            String obsrValue
    ) {

    }
}