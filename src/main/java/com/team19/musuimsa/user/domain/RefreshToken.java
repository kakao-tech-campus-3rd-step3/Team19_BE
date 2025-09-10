package com.team19.musuimsa.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    public static final String BEARER_TYPE = "Bearer ";

    @Column(name = "refresh_token")
    private String token;

    private RefreshToken(String token) {
        this.token = token;
    }

    public static RefreshToken from(String token) {
        return new RefreshToken(token);
    }

    public String getPureToken() {
        if (StringUtils.hasText(token) && token.startsWith(BEARER_TYPE)) {
            return token.substring(BEARER_TYPE.length());
        }

        return token;
    }
}
