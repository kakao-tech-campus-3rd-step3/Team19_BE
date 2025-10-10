package com.team19.musuimsa.wish.repository;

import com.team19.musuimsa.wish.domain.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    Optional<Wish> findByUserUserIdAndShelterShelterId(Long userId, Long shelterId);

    void deleteByUserUserIdAndShelterShelterId(Long userId, Long shelterId);

    @Query("""
              select w from Wish w
              join fetch w.shelter s
              where w.user.userId = :userId
              order by w.createdAt desc
            """)
    List<Wish> findAllWithShelterByUserIdOrderByCreatedAtDesc(Long userId);
}
