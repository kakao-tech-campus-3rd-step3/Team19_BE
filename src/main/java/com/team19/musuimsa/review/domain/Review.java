package com.team19.musuimsa.review.domain;

import com.team19.musuimsa.audit.BaseEntity;
import com.team19.musuimsa.exception.forbidden.ReviewAccessDeniedException;
import com.team19.musuimsa.review.dto.CreateReviewRequest;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelter_id", nullable = false)
    private Shelter shelter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String photoUrl;

    @Column(columnDefinition = "text")
    private String content;

    @Column(nullable = false, columnDefinition = "TINYINT")
    @Min(1)
    @Max(5)
    private int rating;

    public static Review of(Shelter shelter, User user, CreateReviewRequest request) {
        return new Review(null, shelter, user, request.photoUrl(),
                request.content(), request.rating());
    }

    public void update(String content, Integer rating, String photoUrl) {
        if (content != null) {
            this.content = content;
        }

        if (rating != null) {
            this.rating = rating;
        }

        if (photoUrl != null) {
            this.photoUrl = photoUrl;
        }
    }

    // 리뷰 소유자인지 검증
    public void assertOwnedBy(User user) {
        if (!this.user.getUserId().equals(user.getUserId())) {
            throw new ReviewAccessDeniedException();
        }
    }
}
