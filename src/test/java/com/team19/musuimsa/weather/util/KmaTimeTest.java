package com.team19.musuimsa.weather.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KmaTimeTest {

    private final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("latestBase: 정시 11분 미만 - 이전 시간 자료 반환")
    void latestBase_Before11Minutes() {
        // KST 15:05 (11분 미만이므로 14:00 자료를 반환해야 함)
        Clock clock = Clock.fixed(Instant.parse("2025-10-03T06:05:00Z"), KST);

        KmaTime.Base base = KmaTime.latestBase(clock);

        assertEquals("20251003", base.date());
        assertEquals("1400", base.time()); // 15시 - 1시간 = 14시
    }

    @Test
    @DisplayName("latestBase: 정시 11분 이후 - 현재 시간 자료 반환")
    void latestBase_After11Minutes() {
        // KST 15:15 (11분 이후이므로 15:00 자료를 반환해야 함)
        Clock clock = Clock.fixed(Instant.parse("2025-10-03T06:15:00Z"), KST);

        KmaTime.Base base = KmaTime.latestBase(clock);

        assertEquals("20251003", base.date());
        assertEquals("1500", base.time());
    }

    @Test
    @DisplayName("minusHours: 2시간 전 시간 계산")
    void minusHours_Subtract2Hours() {
        KmaTime.Base current = new KmaTime.Base("20251003", "1400");

        KmaTime.Base prev = KmaTime.minusHours(current, 2);

        assertEquals("20251003", prev.date());
        assertEquals("1200", prev.time());
    }

    @Test
    @DisplayName("minusHours: 자정을 넘어가는 시간 계산")
    void minusHours_CrossMidnight() {
        KmaTime.Base current = new KmaTime.Base("20251003", "0100");

        KmaTime.Base prev = KmaTime.minusHours(current, 3);

        assertEquals("20251002", prev.date());
        assertEquals("2200", prev.time());
    }
}