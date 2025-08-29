package com.team19.musuimsa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
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
                        .requestMatchers("/api/users/**").permitAll()

                        // 그 외의 요청은 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
