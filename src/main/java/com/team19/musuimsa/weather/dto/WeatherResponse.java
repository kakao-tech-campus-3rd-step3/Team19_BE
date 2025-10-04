package com.team19.musuimsa.weather.dto;

public record WeatherResponse(
        double temperature,
        String baseDate,
        String baseTime
) {

}