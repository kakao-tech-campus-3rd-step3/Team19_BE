package com.team19.musuimsa.shelter.repository;

import com.team19.musuimsa.shelter.domain.Shelter;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    /**
     * Haversine (미터). 반경 내 정렬 + 상한 100.
     * RDB: MySQL/H2 모두 동작 가능한 식 사용
     */
    @Query(value = """
        SELECT s.shelter_id AS shelterId,
               s.name AS name,
               s.address AS address,
               s.latitude AS latitude,
               s.longitude AS longitude,
               s.is_outdoors AS isOutdoors,
               s.weekday_open_time AS weekdayOpenTime,
               s.weekday_close_time AS weekdayCloseTime,
               s.weekend_open_time AS weekendOpenTime,
               s.weekend_close_time AS weekendCloseTime,
               s.total_rating AS totalRating,
               s.review_count AS reviewCount,
               s.photo_url AS photoUrl,
               (6371000 * acos(
                   cos(radians(:lat)) * cos(radians(s.latitude)) *
                   cos(radians(s.longitude) - radians(:lon)) +
                   sin(radians(:lat)) * sin(radians(s.latitude))
               )) AS distanceM
        FROM shelters s
        HAVING distanceM <= :radius
        ORDER BY distanceM ASC
        LIMIT 100
        """, nativeQuery = true)
    List<ShelterNearbyRow> findNearby(@Param("lat") double lat,
                                      @Param("lon") double lon,
                                      @Param("radius") double radiusMeters);

    interface ShelterNearbyRow {
        Long getShelterId();
        String getName();
        String getAddress();
        BigDecimal getLatitude();
        BigDecimal getLongitude();
        Boolean getIsOutdoors();
        java.sql.Time getWeekdayOpenTime();
        java.sql.Time getWeekdayCloseTime();
        java.sql.Time getWeekendOpenTime();
        java.sql.Time getWeekendCloseTime();
        Integer getTotalRating();
        Integer getReviewCount();
        String getPhotoUrl();
        Double getDistanceM();
    }
}
