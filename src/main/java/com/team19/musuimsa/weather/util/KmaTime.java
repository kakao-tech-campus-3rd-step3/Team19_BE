package com.team19.musuimsa.weather.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class KmaTime {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter D8 = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter H4 = DateTimeFormatter.ofPattern("HHmm");

    private KmaTime() {
    }

    public static Base latestBase(Clock clock) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime hour = now.withMinute(0).withSecond(0).withNano(0);
        if (now.getMinute() < 11) {
            hour = hour.minusHours(1);
        }
        return new Base(D8.format(hour), H4.format(hour));
    }
    
    public static Base minusHours(Base b, int h) {
        ZonedDateTime z = ZonedDateTime.of(
                LocalDate.parse(b.date(), D8).atTime(Integer.parseInt(b.time().substring(0, 2)), 0),
                KST
        ).minusHours(h);
        return new Base(D8.format(z), H4.format(z));
    }

    public record Base(String date, String time) {

    }
}