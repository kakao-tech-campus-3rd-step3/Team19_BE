package com.team19.musuimsa.shelter.repository;

import com.team19.musuimsa.shelter.domain.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
