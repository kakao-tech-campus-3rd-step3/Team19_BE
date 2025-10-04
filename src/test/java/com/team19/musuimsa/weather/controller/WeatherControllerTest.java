package com.team19.musuimsa.weather.controller;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team19.musuimsa.weather.dto.WeatherResponse;
import com.team19.musuimsa.weather.service.WeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;

@WebMvcTest(
        controllers = WeatherController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class},
        excludeFilters = @Filter(type = FilterType.ANNOTATION, classes = ControllerAdvice.class)
)
public class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WeatherService weatherService;

    @Test
    @DisplayName("/api/weather/current 호출 시 200 OK와 날씨 정보를 반환한다")
    void getCurrentTemp_ReturnsWeatherResponse_200OK() throws Exception {
        // Given
        WeatherResponse mockResponse = new WeatherResponse(30, "20251003", "1500");

        given(weatherService.getCurrentTemp(anyDouble(), anyDouble())).willReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/weather/current")
                        .param("latitude", String.valueOf(36.3504))
                        .param("longitude", String.valueOf(127.3845))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.temperature").value(30))
                .andExpect(jsonPath("$.baseDate").value("20251003"))
                .andExpect(jsonPath("$.baseTime").value("1500"));
    }
}