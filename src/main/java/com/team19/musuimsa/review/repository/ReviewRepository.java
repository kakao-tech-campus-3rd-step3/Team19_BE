package com.team19.musuimsa.review.repository;

import com.team19.musuimsa.review.domain.Review;
import com.team19.musuimsa.review.dto.ShelterReviewCountAndSum;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.user.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByShelterOrderByCreatedAtDesc(Shelter shelter);

    List<Review> findByUser(User user);

    @Query("""
                select new com.team19.musuimsa.review.dto.ShelterReviewCountAndSum(
                    count(r),
                    coalesce(sum(r.rating), 0)
                )
                from Review r
                where r.shelter.shelterId = :shelterId
            """)
    ShelterReviewCountAndSum aggregateByShelterId(@Param("shelterId") Long shelterId);
}