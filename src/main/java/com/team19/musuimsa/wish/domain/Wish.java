package com.team19.musuimsa.wish.domain;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishes",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_shelter", columnNames = {"user_id", "shelter_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wishId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelter_id", nullable = false)
    private Shelter shelter;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Wish(User user, Shelter shelter) {
        this.user = user;
        this.shelter = shelter;
        this.createdAt = LocalDateTime.now();
    }

    public static Wish of(User user, Shelter shelter) {
        return new Wish(user, shelter);
    }
}

