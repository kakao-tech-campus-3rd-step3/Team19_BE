package com.team19.musuimsa.review.repository;

import com.team19.musuimsa.review.domain.Review;
import com.team19.musuimsa.user.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByShelterOrderByCreatedAtDesc(Shelter shelter);

    List<Review> findByUser(User user);

}
