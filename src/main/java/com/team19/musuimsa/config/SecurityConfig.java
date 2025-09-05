package com.team19.musuimsa.config;

import com.team19.musuimsa.filter.JwtAuthorizationFilter;
import com.team19.musuimsa.security.UserDetailsServiceImpl;
import com.team19.musuimsa.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter() {
        return new JwtAuthorizationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API 서버는 토큰 기반으로 인증, 서버가 직접 제공하는 로그인 화면이나 브라우저 인증 팝업 필요 없음
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // 세션 대신 JWT 토큰을 사용하는 방식이므로 CSRF 공격에 대한 방어가 필요 없음
                .csrf(AbstractHttpConfigurer::disable)

                // JWT 토큰을 사용하는 경우 세션을 생성하지 않음
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인증이 필요한 요청에 대해서는 인증을 요구함
                .authorizeHttpRequests(auth -> auth
                        // 회원가입과 로그인 API는 모두 허용
                        .requestMatchers("/api/users/signup", "/api/users/login",
                                "/api/users/reissue", "/shelters/{shelterId}/reviews").permitAll()
                        // 특정 사용자를 조회하는 GET 요청은 허용
                        .requestMatchers(HttpMethod.GET, "/api/users/{userId}").permitAll()
                        // 그 외의 모든 /api/users/** 요청은 인증된 사용자만 접근 가능
                        .requestMatchers("/api/users/**", "/reviews/{reviewId}",
                                "/reviews/{reviewId}", "/users/me/reviews")
                        .authenticated()

                        // 나머지 요청은 일단 모두 허용 (추후에 필요에 따라 변경 가능)
                        .anyRequest().permitAll()
                );

        // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 전에 추가
        http.addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}