package com.team19.musuimsa.shelter.util;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.OperatingHoursResponse;
import com.team19.musuimsa.shelter.dto.ShelterResponse;

import java.time.LocalTime;

public final class ShelterDtoUtils {
    private ShelterDtoUtils() {
    }

    public static String formatHours(LocalTime open, LocalTime close) {
        if (open == null && close == null) {
            return "";
        }

        String start = (open != null) ? String.format("%02d:%02d", open.getHour(), open.getMinute()) : "";
        String end = (close != null) ? String.format("%02d:%02d", close.getHour(), close.getMinute()) : "";
        return start + "~" + end;
    }

    public static double average(Integer total, Integer count) {
        if (total == null || count == null || count == 0) {
            return 0.0;
        }
        return total.doubleValue() / count;
    }

    public static NearbyShelterResponse toNearbyDto(Shelter s /*, Double distanceOpt */) {
        return new NearbyShelterResponse(
                s.getShelterId(),
                s.getName(),
                s.getAddress(),
                s.getLatitude().doubleValue(),
                s.getLongitude().doubleValue(),
                s.getIsOutdoors(),
                new OperatingHoursResponse(
                        formatHours(s.getWeekdayOpenTime(), s.getWeekdayCloseTime()),
                        formatHours(s.getWeekendOpenTime(), s.getWeekendCloseTime())
                ),
                average(s.getTotalRating(), s.getReviewCount()),
                s.getPhotoUrl()
        );
    }

    public static ShelterResponse toDetailDto(Shelter s) {
        return new ShelterResponse(
                s.getShelterId(),
                s.getName(),
                s.getAddress(),
                s.getLatitude().doubleValue(),
                s.getLongitude().doubleValue(),
                new OperatingHoursResponse(
                        formatHours(s.getWeekdayOpenTime(), s.getWeekdayCloseTime()),
                        formatHours(s.getWeekendOpenTime(), s.getWeekendCloseTime())
                ),
                s.getCapacity(),
                s.getIsOutdoors(),
                new ShelterResponse.CoolingEquipment(s.getFanCount(), s.getAirConditionerCount()),
                s.getTotalRating(),
                s.getReviewCount(),
                s.getPhotoUrl()
        );
    }
}
