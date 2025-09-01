package com.team19.musuimsa.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("dev") // dev 프로필에서만 활성화
public class H2ConsoleConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        // 이 설정은 /h2-console/** 경로에만 적용됩니다.
        http.securityMatcher(PathRequest.toH2Console());

        http
                // h2-console에 대한 모든 요청을 허용
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // h2-console은 CSRF 보호가 필요 없음
                .csrf(AbstractHttpConfigurer::disable)
                // h2-console은 프레임 안에서 실행되므로 X-Frame-Options 비활성화
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        return http.build();
    }
}
