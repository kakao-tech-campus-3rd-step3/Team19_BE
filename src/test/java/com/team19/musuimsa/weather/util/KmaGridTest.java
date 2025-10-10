package com.team19.musuimsa.weather.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.team19.musuimsa.weather.dto.NxNy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class KmaGridTest {

    // 1. 제주도 좌표
    // 위도: 33.511389, 경도: 126.493889
    // 예상 격자 좌표 : NX=52, NY=38
    private static final double JEJU_LAT = 33.511389;
    private static final double JEJU_LON = 126.493889;
    private static final int JEJU_NX = 52;
    private static final int JEJU_NY = 38;

    // 2. 대전 좌표
    // 위도: 36.3504119, 경도: 127.3845475
    // 예상 격자 좌표 : NX=67, NY=100
    private static final double DAEJEON_LAT = 36.3504119;
    private static final double DAEJEON_LON = 127.3845475;
    private static final int DAEJEON_NX = 67;
    private static final int DAEJEON_NY = 100;

    @Test
    @DisplayName("제주도 좌표를 KMA 격자 좌표로 정확히 반환한다.")
    void convertJejuToGridCorrectly() {
        // when
        NxNy result = KmaGrid.fromLatLon(JEJU_LAT, JEJU_LON);

        // then
        assertNotNull(result);
        assertEquals(JEJU_NX, result.nx());
        assertEquals(JEJU_NY, result.ny());
    }

    @Test
    @DisplayName("대전 좌표를 KMA 격자 좌표로 정확히 반환한다.")
    void convertDaejeonToGridCorrectly() {
        // when
        NxNy result = KmaGrid.fromLatLon(DAEJEON_LAT, DAEJEON_LON);

        // then
        assertNotNull(result);
        assertEquals(DAEJEON_NX, result.nx());
        assertEquals(DAEJEON_NY, result.ny());
    }
}