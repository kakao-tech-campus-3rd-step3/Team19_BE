package com.team19.musuimsa.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team19.musuimsa.exception.handler.GlobalExceptionHandler;
import com.team19.musuimsa.review.dto.CreateReviewRequest;
import com.team19.musuimsa.review.dto.ReviewResponse;
import com.team19.musuimsa.review.dto.UpdateReviewRequest;
import com.team19.musuimsa.review.service.ReviewService;
import com.team19.musuimsa.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
public class ReviewControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private User user;
    private ReviewResponse response;
    private Long shelterId;
    private Long reviewId;

    @BeforeEach
    void setUp() {
        shelterId = 10L;
        reviewId = 100L;

        user = new User("aran@email.com", "1234", "별명", "프사.url");

        response = new ReviewResponse(reviewId, shelterId, "무더위쉼터", user.getUserId(),
                "아란", "시원하네요", 5,
                "photo.url", "profile.url",
                LocalDateTime.now(), LocalDateTime.now());

        objectMapper.registerModule(new JavaTimeModule());

        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setCustomArgumentResolvers(new MockAuthenticationPrincipalArgumentResolver(user))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jacksonConverter,
                        new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .build();
    }

    // @AuthenticationPrincipal에 사용자 객체를 주입하기 위한 커스텀 ArgumentResolver
    private static class MockAuthenticationPrincipalArgumentResolver implements
            HandlerMethodArgumentResolver {

        private final User user;

        public MockAuthenticationPrincipalArgumentResolver(User user) {
            this.user = user;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterAnnotation(AuthenticationPrincipal.class) != null &&
                    parameter.getParameterType().equals(User.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
            return user;
        }
    }

    @Test
    @DisplayName("리뷰 생성 요청 성공")
    void createReviewSsuccess() throws Exception {
        // Given
        CreateReviewRequest request = new CreateReviewRequest("시원하네요", 5, "phtoturl");

        given(reviewService.createReview(any(Long.class), any(CreateReviewRequest.class),
                any(User.class)))
                .willReturn(response);

        // When & Then
        mockMvc.perform(post("/api/shelters/{shelterId}/reviews", shelterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/reviews/" + response.reviewId()));

        verify(reviewService).createReview(any(Long.class), any(CreateReviewRequest.class),
                any(User.class));
    }

    @Test
    @DisplayName("리뷰 내용, 사진 URL 없이도 생성 요청 성공")
    void createReviewWithNoContentAndNoUrlSsuccess() throws Exception {
        // Given
        CreateReviewRequest request = new CreateReviewRequest("", 5, "");

        given(reviewService.createReview(any(Long.class), any(CreateReviewRequest.class),
                any(User.class)))
                .willReturn(response);

        // When & Then
        mockMvc.perform(post("/api/shelters/{shelterId}/reviews", shelterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/reviews/" + response.reviewId()));

        verify(reviewService).createReview(any(Long.class), any(CreateReviewRequest.class),
                any(User.class));
    }

    @Test
    @DisplayName("리뷰 수정 요청 성공")
    void updateReviewSuccess() throws Exception {
        // Given
        UpdateReviewRequest request = new UpdateReviewRequest("수정된 리뷰 내용입니다.", 4, "photoUrl");

        given(reviewService.updateReview(any(Long.class), any(UpdateReviewRequest.class),
                any(User.class)))
                .willReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(reviewService).updateReview(any(Long.class), any(UpdateReviewRequest.class),
                any(User.class));
    }

    @Test
    @DisplayName("별점만 변경 요청 성공")
    void updateReviewRatingSuccess() throws Exception {
        // Given
        UpdateReviewRequest request = new UpdateReviewRequest(null, 4, null);

        given(reviewService.updateReview(any(Long.class), any(UpdateReviewRequest.class),
                any(User.class)))
                .willReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(reviewService).updateReview(any(Long.class), any(UpdateReviewRequest.class),
                any(User.class));
    }

    @Test
    @DisplayName("photoUrl만 변경 요청 성공")
    void updateReviewPhotoUrlSuccess() throws Exception {
        // Given
        UpdateReviewRequest request = new UpdateReviewRequest(null, null, "updateUrl");

        given(reviewService.updateReview(any(Long.class), any(UpdateReviewRequest.class),
                any(User.class)))
                .willReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(reviewService).updateReview(any(Long.class), any(UpdateReviewRequest.class),
                any(User.class));
    }

    @Test
    @DisplayName("리뷰 삭제 요청 성공")
    void deleteReviewSuccess() throws Exception {
        // Given
        doNothing().when(reviewService).deleteReview(any(Long.class), any(User.class));

        // When & Then
        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(any(Long.class), any(User.class));
    }

    @Test
    @DisplayName("리뷰 단건 조회 요청 성공")
    void getReviewSuccess() throws Exception {
        // Given
        given(reviewService.getReview(any(Long.class))).willReturn(response);

        // When & Then
        mockMvc.perform(get("/api/reviews/{reviewId}", reviewId))
                .andExpect(status().isOk());

        verify(reviewService).getReview(any(Long.class));
    }

    @Test
    @DisplayName("쉼터별 리뷰 조회 요청 성공")
    void getReviewByShelterSuccess() throws Exception {
        // Given
        List<ReviewResponse> reviews = List.of(response);

        given(reviewService.getReviewsByShelter(any(Long.class))).willReturn(reviews);

        // When & Then
        mockMvc.perform(get("/api/shelters/{shelterId}/reviews", shelterId))
                .andExpect(status().isOk());

        verify(reviewService).getReviewsByShelter(any(Long.class));
    }

    @Test
    @DisplayName("사용자별 리뷰 조회 요청 성공")
    void getReviewByUserSuccess() throws Exception {
        // Given
        List<ReviewResponse> reviews = List.of(response);

        given(reviewService.getReviewsByUser(any(User.class))).willReturn(reviews);

        // When & Then
        mockMvc.perform(get("/api/users/me/reviews"))
                .andExpect(status().isOk());

        verify(reviewService).getReviewsByUser(any(User.class));
    }
}