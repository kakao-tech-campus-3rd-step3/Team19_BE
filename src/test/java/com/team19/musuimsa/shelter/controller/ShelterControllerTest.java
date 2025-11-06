package com.team19.musuimsa.shelter.controller;

import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.OperatingHoursResponse;
import com.team19.musuimsa.shelter.dto.ShelterResponse;
import com.team19.musuimsa.shelter.dto.map.ClusterFeature;
import com.team19.musuimsa.shelter.dto.map.MapFeature;
import com.team19.musuimsa.shelter.dto.map.MapResponse;
import com.team19.musuimsa.shelter.dto.map.MapShelterResponse;
import com.team19.musuimsa.shelter.service.ShelterMapService;
import com.team19.musuimsa.shelter.service.ShelterService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShelterController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShelterControllerTest {

    @Resource
    MockMvc mockMvc;

    @MockitoBean
    ShelterService shelterService;

    @MockitoBean
    ShelterMapService shelterMapService;

    @Test
    @DisplayName("GET /api/shelters - 바운딩박스 파라미터 바인딩 및 MapResponse JSON 반환")
    void getByBbox_returnsMapResponse() throws Exception {
        List<MapFeature> items = List.of(
                new ClusterFeature("gh_1", 37.11, 127.11, 3),
                new MapShelterResponse(1L, "중앙 쉼터", "서울 주소", 37.5665, 126.9780, "0.5km", true, 50, "u.jpg", new OperatingHoursResponse("09:00~18:00", "10:00~16:00"), 4.0)
        );
        Mockito.when(shelterMapService.getByBbox(Mockito.any()))
                .thenReturn(new MapResponse("cluster", items, 42));

        mockMvc.perform(get("/api/shelters")
                        .param("minLat", "37.0")
                        .param("minLng", "127.0")
                        .param("maxLat", "37.2")
                        .param("maxLng", "127.2")
                        .param("userLat", "37.5665")
                        .param("userLng", "126.9780")
                        .param("zoom", "12")
                        .param("page", "0")
                        .param("size", "200")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level", is("cluster")))
                .andExpect(jsonPath("$.total", is(42)))
                .andExpect(jsonPath("$.items", hasSize(2)));

        verify(shelterMapService).getByBbox(Mockito.argThat(
                req -> req.userLat() != null && req.userLat() == 37.5665
                        && req.userLng() != null && req.userLng() == 126.9780
        ));
    }

    @DisplayName("GET /api/shelters/nearby - 가까운 쉼터 목록 JSON 반환")
    @Test
    void getNearby_returnsList() throws Exception {
        List<NearbyShelterResponse> stub = List.of(
                new NearbyShelterResponse(
                        1L, "종로 무더위 쉼터", "서울 종로구 세종대로 175",
                        37.5665, 126.9780, "0m", true,
                        new OperatingHoursResponse("09:00~18:00", "10:00~16:00"),
                        4.5, "https://example.com/shelter1.jpg"
                ),
                new NearbyShelterResponse(
                        2L, "을지로 무더위 쉼터", "서울 중구 을지로 45",
                        37.5651, 126.9895, "1.1km", false,
                        new OperatingHoursResponse("09:00~18:00", "10:00~16:00"),
                        3.8, "https://example.com/shelter2.jpg"
                )
        );
        Mockito.when(shelterService.findNearbyShelters(anyDouble(), anyDouble()))
                .thenReturn(stub);

        mockMvc.perform(get("/api/shelters/nearby")
                        .param("latitude", "37.5")
                        .param("longitude", "127.0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                // 첫번째 무더위 쉼터
                .andExpect(jsonPath("$[0].shelterId", is(1)))
                .andExpect(jsonPath("$[0].name", is("종로 무더위 쉼터")))
                .andExpect(jsonPath("$[0].address", is("서울 종로구 세종대로 175")))
                .andExpect(jsonPath("$[0].latitude", is(37.5665)))
                .andExpect(jsonPath("$[0].longitude", is(126.9780)))
                .andExpect(jsonPath("$[0].distance", is("0m")))
                .andExpect(jsonPath("$[0].isOutdoors", is(true)))
                .andExpect(jsonPath("$[0].operatingHours.weekday", is("09:00~18:00")))
                .andExpect(jsonPath("$[0].operatingHours.weekend", is("10:00~16:00")))
                .andExpect(jsonPath("$[0].averageRating", is(4.5)))
                .andExpect(jsonPath("$[0].photoUrl", is("https://example.com/shelter1.jpg")))
                // 두번째 무더위 쉼터
                .andExpect(jsonPath("$[1].shelterId", is(2)))
                .andExpect(jsonPath("$[1].name", is("을지로 무더위 쉼터")))
                .andExpect(jsonPath("$[1].address", is("서울 중구 을지로 45")))
                .andExpect(jsonPath("$[1].latitude", is(37.5651)))
                .andExpect(jsonPath("$[1].longitude", is(126.9895)))
                .andExpect(jsonPath("$[1].distance", is("1.1km")))
                .andExpect(jsonPath("$[1].isOutdoors", is(false)))
                .andExpect(jsonPath("$[1].operatingHours.weekday", is("09:00~18:00")))
                .andExpect(jsonPath("$[1].operatingHours.weekend", is("10:00~16:00")))
                .andExpect(jsonPath("$[1].averageRating", is(3.8)))
                .andExpect(jsonPath("$[1].photoUrl", is("https://example.com/shelter2.jpg")));
    }

    @DisplayName("GET /api/shelters/{shelterId} - 상세 쉼터 JSON 반환")
    @Test
    void getDetail_returnsOne() throws Exception {
        ShelterResponse dto = new ShelterResponse(
                1L, "종로 무더위쉼터", "서울 종로구 세종대로 175", 37.5665, 126.9780, "0m",
                new OperatingHoursResponse("09:00~18:00", "10:00~16:00"),
                50, true,
                new ShelterResponse.CoolingEquipment(3, 1),
                14, 5, "https://example.com/shelter1.jpg"
        );

        Mockito.when(shelterService.getShelter(eq(1L), eq(37.5), eq(127.0))).thenReturn(dto);

        mockMvc.perform(get("/api/shelters/{shelterId}", 1L)
                        .param("latitude", "37.5")
                        .param("longitude", "127.0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("종로 무더위쉼터")))
                .andExpect(jsonPath("$.address", is("서울 종로구 세종대로 175")))
                .andExpect(jsonPath("$.latitude", is(37.5665)))
                .andExpect(jsonPath("$.longitude", is(126.9780)))
                .andExpect(jsonPath("$.distance", is("0m")))
                .andExpect(jsonPath("$.operatingHours.weekday", is("09:00~18:00")))
                .andExpect(jsonPath("$.operatingHours.weekend", is("10:00~16:00")))
                .andExpect(jsonPath("$.capacity", is(50)))
                .andExpect(jsonPath("$.isOutdoors", is(true)))
                .andExpect(jsonPath("$.coolingEquipment.fanCount", is(3)))
                .andExpect(jsonPath("$.coolingEquipment.airConditionerCount", is(1)))
                .andExpect(jsonPath("$.totalRating", is(14)))
                .andExpect(jsonPath("$.reviewCount", is(5)))
                .andExpect(jsonPath("$.photoUrl", is("https://example.com/shelter1.jpg")));
    }
}
