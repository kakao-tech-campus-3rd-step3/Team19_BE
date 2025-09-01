package com.team19.musuimsa.filter;

import com.team19.musuimsa.security.UserDetailsImpl;
import com.team19.musuimsa.security.UserDetailsService;
import com.team19.musuimsa.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            if (jwtUtil.validateToken(token)) {
                Claims userInfo = jwtUtil.getUserInfoFromToken(token);
                String email = userInfo.getSubject();

                try {
                    setAuthentication(email);
                } catch (Exception e) {
                    log.error("Authentication error: {}", e.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Authentication failed");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    // SecurityContext에 인증 정보 설정
    private void setAuthentication(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(
                email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    // 요청 헤더에서 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(JwtUtil.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(JwtUtil.BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
