package com.team19.musuimsa.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String profileImageUrl;

    @Embedded
    private RefreshToken refreshToken;

    @Column(precision = 10, scale = 8)
    private BigDecimal lastLatitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal lastLongitude;

    private LocalDateTime lastHeatwaveAlertAt;

    public User(String email, String password, String nickname, String profileImageUrl) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = RefreshToken.from(refreshToken);
    }

    public void updateUser(String nickname, String profileImageUrl) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void invalidateRefreshToken() {
        this.refreshToken = null;
    }

    public void updateLocation(BigDecimal latitude, BigDecimal longitude) {
        this.lastLatitude = latitude;
        this.lastLongitude = longitude;
    }

    public void updateLastHeatwaveAlertAt() {
        this.lastHeatwaveAlertAt = LocalDateTime.now();
    }
}
