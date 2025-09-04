package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.exception.notfound.ShelterNotFoundException;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.ShelterDetailResponse;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShelterService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final ShelterRepository shelterRepository;

    public List<NearbyShelterResponse> findNearby(double lat, double lon, double radiusMeters) {
        List<ShelterRepository.ShelterNearbyRow> rows = shelterRepository.findNearby(lat, lon, radiusMeters);
        LocalDateTime now = LocalDateTime.now(ZONE);

        return rows.stream().map(r -> {
            boolean opened = isOpened(
                    toLocalTime(r.getWeekdayOpenTime()), toLocalTime(r.getWeekdayCloseTime()),
                    toLocalTime(r.getWeekendOpenTime()), toLocalTime(r.getWeekendCloseTime()),
                    now);

            double avg = calcAverage(r.getTotalRating(), r.getReviewCount());
            String distance = formatDistance(r.getDistanceM());

            Map<String, String> hours = Map.of(
                    "weekday", formatHours(toLocalTime(r.getWeekdayOpenTime()), toLocalTime(r.getWeekdayCloseTime())),
                    "weekend", formatHours(toLocalTime(r.getWeekendOpenTime()), toLocalTime(r.getWeekendCloseTime()))
            );

            double latitude = (r.getLatitude()  == null) ? 0.0 : r.getLatitude().doubleValue();
            double longitude = (r.getLongitude() == null) ? 0.0 : r.getLongitude().doubleValue();

            return new NearbyShelterResponse(
                    r.getShelterId(),
                    r.getName(),
                    r.getAddress(),
                    latitude,
                    longitude,
                    distance,
                    opened,
                    Boolean.TRUE.equals(r.getIsOutdoors()),
                    hours,
                    avg,
                    r.getPhotoUrl()
            );
        }).collect(Collectors.toList());
    }

    public ShelterDetailResponse getShelter(Long shelterId) {
        Shelter s = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        LocalDateTime now = LocalDateTime.now(ZONE);
        boolean opened = isOpened(
                s.getWeekdayOpenTime(), s.getWeekdayCloseTime(),
                s.getWeekendOpenTime(), s.getWeekendCloseTime(),
                now);

        Map<String, String> hours = Map.of(
                "weekday", formatHours(s.getWeekdayOpenTime(), s.getWeekdayCloseTime()),
                "weekend", formatHours(s.getWeekendOpenTime(), s.getWeekendCloseTime())
        );

        double latitude = (s.getLatitude()  == null) ? 0.0 : s.getLatitude().doubleValue();
        double longitude = (s.getLongitude() == null) ? 0.0 : s.getLongitude().doubleValue();

        return new ShelterDetailResponse(
                s.getShelterId(),
                s.getName(),
                s.getAddress(),
                latitude,
                longitude,
                hours,
                s.getCapacity(),
                opened,
                Boolean.TRUE.equals(s.getIsOutdoors()),
                new ShelterDetailResponse.CoolingEquipment(s.getFanCount(), s.getAirConditionerCount()),
                Optional.ofNullable(s.getTotalRating()).orElse(0),
                Optional.ofNullable(s.getReviewCount()).orElse(0),
                s.getPhotoUrl()
        );
    }

    private static LocalTime toLocalTime(java.sql.Time t) {
        return t == null ? null : t.toLocalTime();
    }

    private static String formatHours(LocalTime open, LocalTime close) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("HH:mm");
        if (open == null || close == null) {
            return "-";
        }
        return open.format(f) + "–" + close.format(f);
    }

    private static String formatDistance(Double meters) {
        if (meters == null) {
            return "";
        }
        return meters < 1000 ? Math.round(meters) + "m" : String.format("%.1fkm", meters / 1000.0);
    }

    private static double calcAverage(Integer total, Integer count) {
        int t = Optional.ofNullable(total).orElse(0);
        int c = Optional.ofNullable(count).orElse(0);
        if (c == 0) {
            return 0.0;
        }
        return Math.round((t / (double) c) * 10.0) / 10.0;
    }

    // 평일/주말 시간대로 개점 여부 계산(야간 넘어가는 구간도 처리)
    private static boolean isOpened(LocalTime wkOpen, LocalTime wkClose,
                                    LocalTime weOpen, LocalTime weClose,
                                    LocalDateTime now) {
        DayOfWeek d = now.getDayOfWeek();
        boolean weekend = (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY);
        LocalTime open = weekend ? weOpen : wkOpen;
        LocalTime close = weekend ? weClose : wkClose;
        if (open == null || close == null) {
            return false;
        }

        LocalTime t = now.toLocalTime();
        if (open.equals(close)) {
            return false; // 휴무로 처리
        }
        if (close.isBefore(open)) { // 21:00–02:00 같은 구간
            return !t.isBefore(open) || !t.isAfter(close);
        }
        return !t.isBefore(open) && !t.isAfter(close);
    }
}
