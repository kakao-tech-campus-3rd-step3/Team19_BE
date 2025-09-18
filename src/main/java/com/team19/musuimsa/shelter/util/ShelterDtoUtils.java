package com.team19.musuimsa.shelter.util;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.OperatingHoursResponse;
import com.team19.musuimsa.shelter.dto.ShelterResponse;

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

    public static String formatDistance(double meters) {
        if (meters <= 0) {
            return "0.0km";
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
                shelter.getLatitude().doubleValue(), shelter.getLongitude().doubleValue()
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
        if (count == 0) {
            return 0.0;
        }
        return total.doubleValue() / count;
    }

    public static NearbyShelterResponse toNearbyDto(Shelter shelter, String distance) {
        return new NearbyShelterResponse(
                shelter.getShelterId(),
                shelter.getName(),
                shelter.getAddress(),
                shelter.getLatitude().doubleValue(),
                shelter.getLongitude().doubleValue(),
                distance,
                shelter.getIsOutdoors(),
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
                shelter.getLatitude().doubleValue(),
                shelter.getLongitude().doubleValue(),
                distance,
                new OperatingHoursResponse(
                        formatHours(shelter.getWeekdayOpenTime(), shelter.getWeekdayCloseTime()),
                        formatHours(shelter.getWeekendOpenTime(), shelter.getWeekendCloseTime())
                ),
                shelter.getCapacity(),
                shelter.getIsOutdoors(),
                new ShelterResponse.CoolingEquipment(shelter.getFanCount(), shelter.getAirConditionerCount()),
                shelter.getTotalRating(),
                shelter.getReviewCount(),
                shelter.getPhotoUrl()
        );
    }
}
