package com.team19.musuimsa.shelter.util;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.OperatingHoursResponse;
import com.team19.musuimsa.shelter.dto.ShelterResponse;

import java.math.BigDecimal;
import java.time.LocalTime;

public final class ShelterDtoUtils {
    private ShelterDtoUtils() {
    }

    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static String formatDistance(Double meters) {
        if (meters == null) {
            return null;
        }
        int m = (int) Math.round(meters);
        if (m < 1000) {
            return m + "m";
        }
        double km = meters / 1000.0;
        return String.format(java.util.Locale.US, "%.1fkm", km);
    }

    public static String distanceBetween(double lat1, double lng1, double lat2, double lng2) {
        return formatDistance(haversineMeters(lat1, lng1, lat2, lng2));
    }

    public static String distanceFrom(double userLat, double userLng, Shelter shelter) {
        return distanceBetween(
                userLat, userLng,
                toDoubleOrZero(shelter.getLatitude()),
                toDoubleOrZero(shelter.getLongitude())
        );
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
        int c = toIntOrZero(count);

        if (c == 0) {
            return 0.0;
        }
        return toIntOrZero(total) / (double) c;
    }

    public static NearbyShelterResponse toNearbyDto(Shelter shelter, String distance) {
        return new NearbyShelterResponse(
                shelter.getShelterId(),
                shelter.getName(),
                shelter.getAddress(),
                toDoubleOrZero(shelter.getLatitude()),
                toDoubleOrZero(shelter.getLongitude()),
                distance,
                toBooleanOrFalse(shelter.getIsOutdoors()),
                new OperatingHoursResponse(
                        formatHours(shelter.getWeekdayOpenTime(), shelter.getWeekdayCloseTime()),
                        formatHours(shelter.getWeekendOpenTime(), shelter.getWeekendCloseTime())
                ),
                average(shelter.getTotalRating(), shelter.getReviewCount()),
                shelter.getPhotoUrl()
        );
    }

    public static ShelterResponse toDetailDto(Shelter shelter, String distance) {
        return new ShelterResponse(
                shelter.getShelterId(),
                shelter.getName(),
                shelter.getAddress(),
                toDoubleOrZero(shelter.getLatitude()),
                toDoubleOrZero(shelter.getLongitude()),
                distance,
                new OperatingHoursResponse(
                        formatHours(shelter.getWeekdayOpenTime(), shelter.getWeekdayCloseTime()),
                        formatHours(shelter.getWeekendOpenTime(), shelter.getWeekendCloseTime())
                ),
                toIntOrZero(shelter.getCapacity()),
                toBooleanOrFalse(shelter.getIsOutdoors()),
                new ShelterResponse.CoolingEquipment(
                        toIntOrZero(shelter.getFanCount()),
                        toIntOrZero(shelter.getAirConditionerCount())
                ),
                toIntOrZero(shelter.getTotalRating()),
                toIntOrZero(shelter.getReviewCount()),
                shelter.getPhotoUrl()
        );
    }

    // null-safe helpers
    private static int toIntOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static boolean toBooleanOrFalse(Boolean value) {
        return value != null && value;
    }

    private static double toDoubleOrZero(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
