package com.team19.musuimsa.shelter.repository;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.map.MapShelterResponse;
import com.team19.musuimsa.shelter.dto.map.MapShelterRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    @Query(value = """
            SELECT s.*
            FROM shelters s
            WHERE (6371000 * ACOS(
                     COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
                     COS(RADIANS(s.longitude) - RADIANS(:lng)) +
                     SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude))
                  )) <= :radius
            ORDER BY (6371000 * ACOS(
                     COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
                     COS(RADIANS(s.longitude) - RADIANS(:lng)) +
                     SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude))
                  ))
            """, nativeQuery = true)
    List<Shelter> findNearbyShelters(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") int radius
    );

    @Query("""
              select s.shelterId
              from Shelter s
              where (s.photoUrl is null or s.photoUrl = '')
                and s.latitude  is not null
                and s.longitude is not null
              order by s.shelterId asc
            """)
    List<Long> findPendingShelterIds(Pageable pageable);

    @Query("""
              SELECT new com.team19.musuimsa.shelter.dto.map.MapShelterResponse(
                s.shelterId,
                s.name,
                cast(s.latitude  as double),
                cast(s.longitude as double),
                CASE WHEN coalesce(s.airConditionerCount, 0) > 0 THEN true ELSE false END,
                s.capacity,
                s.photoUrl,
                cast(null as string),
                cast(null as string)
              )
              FROM Shelter s
              WHERE s.latitude  BETWEEN :minLat AND :maxLat
                AND s.longitude BETWEEN :minLng AND :maxLng
            """)
    List<MapShelterResponse> findInBbox(
            @Param("minLat") BigDecimal minLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLat") BigDecimal maxLat,
            @Param("maxLng") BigDecimal maxLng,
            Pageable pageable);

    @Query("""
              SELECT new com.team19.musuimsa.shelter.dto.map.MapShelterRow(
                s.shelterId,
                s.name,
                cast(s.latitude  as double),
                cast(s.longitude as double),
                CASE WHEN coalesce(s.airConditionerCount, 0) > 0 THEN true ELSE false END,
                s.capacity,
                s.photoUrl,
                cast(s.weekdayOpenTime  as string),
                cast(s.weekdayCloseTime as string),
                cast(s.weekendOpenTime  as string),
                cast(s.weekendCloseTime as string)
              )
              FROM Shelter s
              WHERE s.latitude  BETWEEN :minLat AND :maxLat
                AND s.longitude BETWEEN :minLng AND :maxLng
            """)
    List<MapShelterRow> findInBboxWithHours(
            @Param("minLat") BigDecimal minLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLat") BigDecimal maxLat,
            @Param("maxLng") BigDecimal maxLng,
            Pageable pageable);

    @Query("""
              SELECT count(s.shelterId)
              FROM Shelter s
              WHERE s.latitude  BETWEEN :minLat AND :maxLat
                AND s.longitude BETWEEN :minLng AND :maxLng
            """)
    int countInBbox(@Param("minLat") BigDecimal minLat,
                    @Param("minLng") BigDecimal minLng,
                    @Param("maxLat") BigDecimal maxLat,
                    @Param("maxLng") BigDecimal maxLng);
}
