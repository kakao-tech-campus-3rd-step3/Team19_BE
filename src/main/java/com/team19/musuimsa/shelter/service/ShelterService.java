package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.exception.notfound.ShelterNotFoundException;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.OperatingHoursResponse;
import com.team19.musuimsa.shelter.dto.ShelterResponse;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelterService {

    private static final int DEFAULT_RADIUS = 1000;

    private final ShelterRepository shelterRepository;

    public List<NearbyShelterResponse> findNearbyShelters(double latitude, double longitude) {
        List<Shelter> shelters = shelterRepository.findNearbyShelters(latitude, longitude, DEFAULT_RADIUS);

        return shelters.stream()
                .map(s -> new NearbyShelterResponse(
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
                        calcAverageRating(s.getTotalRating(), s.getReviewCount()),
                        s.getPhotoUrl()
                ))
                .toList();
    }

    public ShelterResponse getShelter(Long shelterId) {
        Shelter s = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

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
                new ShelterResponse.CoolingEquipment(
                        s.getFanCount(),
                        s.getAirConditionerCount()
                ),
                s.getTotalRating(),
                s.getReviewCount(),
                s.getPhotoUrl()
        );
    }

    // 둘 다 null -> "" (빈 문자열)
    // open null -> "~HH:mm"
    // close null -> "HH:mm~"
    // 둘 다 존재 -> "HH:mm~HH:mm"
    private String formatHours(LocalTime open, LocalTime close) {
        if (open == null && close == null) {
            return "";
        }

        String start = (open != null) ? toHHmm(open) : "";
        String end = (close != null) ? toHHmm(close) : "";

        return start + "~" + end;
    }

    private String toHHmm(LocalTime t) {
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private Double calcAverageRating(Integer totalRating, Integer reviewCount) {
        if (reviewCount == null || reviewCount == 0 || totalRating == null) {
            return 0.0;
        }

        return totalRating.doubleValue() / reviewCount;
    }
}
