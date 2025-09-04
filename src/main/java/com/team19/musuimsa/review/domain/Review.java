package com.team19.musuimsa.review.domain;

import com.team19.musuimsa.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne
    private Shelter shelter;

    @ManyToOne
    private User user;

    private String photoUrl;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Review of(Shelter shelter, User user, String photoUrl, String title, String content, int rating, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Review(null, shelter, user, photoUrl, title, content, rating, createdAt, updatedAt);
    }

    private Review(Long reviewId, Shelter shelter, User user, String photoUrl, String title, String content, int rating, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.reviewId = reviewId;
        this.shelter = shelter;
        this.user = user;
        this.photoUrl = photoUrl;
        this.title = title;
        this.content = content;
        this.rating = rating;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
