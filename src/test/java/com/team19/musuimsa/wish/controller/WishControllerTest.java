package com.team19.musuimsa.wish.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team19.musuimsa.exception.handler.GlobalExceptionHandler;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.wish.dto.CreateWishResponse;
import com.team19.musuimsa.wish.dto.WishListItemResponse;
import com.team19.musuimsa.wish.dto.WishListResponse;
import com.team19.musuimsa.wish.service.WishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WishControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private WishController wishController;

    @Mock
    private WishService wishService;

    private User loginUser;

    @BeforeEach
    void setUp() {
        loginUser = new User("test@example.com", "password123", "testUser", "profile.jpg");
        ReflectionTestUtils.setField(loginUser, "userId", 1L);

        mockMvc = MockMvcBuilders.standaloneSetup(wishController)
                .setCustomArgumentResolvers(new PrincipalResolver(loginUser))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("위시 추가 — 201 Created + Location + DTO 바디")
    void createWish_createdWithLocationAndBody() throws Exception {
        LocalDateTime ts = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        CreateWishResponse expected = new CreateWishResponse(100L, 1L, 10L, ts);
        given(wishService.createWish(anyLong(), any(User.class))).willReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/users/me/wishes/{shelterId}", 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        containsString("/api/users/me/wishes/100")))
                .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        CreateWishResponse body = objectMapper.readValue(json, CreateWishResponse.class);

        assertSoftly(softly -> {
            softly.assertThat(body.wishId()).isEqualTo(100L);
            softly.assertThat(body.userId()).isEqualTo(1L);
            softly.assertThat(body.shelterId()).isEqualTo(10L);
            softly.assertThat(body.createdAt()).isEqualTo(ts);
        });
    }

    @Test
    @DisplayName("위시 목록 조회 — 200 OK + DTO 목록")
    void getWishes_okWithDtoList() throws Exception {
        WishListItemResponse item =
                new WishListItemResponse(10L, "Shelter A", "Seoul", "", 0.0, null, "1.2km");
        WishListResponse expected = new WishListResponse(List.of(item));
        given(wishService.getWishes(any(User.class), any(), any())).willReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/users/me/wishes")
                        .param("latitude", "37.5")
                        .param("longitude", "127.0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        WishListResponse body = objectMapper.readValue(json, WishListResponse.class);

        assertSoftly(softly -> {
            softly.assertThat(body.items()).hasSize(1);
            softly.assertThat(body.items().get(0).shelterId()).isEqualTo(10L);
            softly.assertThat(body.items().get(0).name()).isEqualTo("Shelter A");
            softly.assertThat(body.items().get(0).address()).isEqualTo("Seoul");
            softly.assertThat(body.items().get(0).distance()).isEqualTo("1.2km");
        });
    }

    @Test
    @DisplayName("위시 삭제 — 204 No Content")
    void deleteWish_noContent() throws Exception {
        doNothing().when(wishService).deleteWish(anyLong(), any(User.class));

        mockMvc.perform(delete("/api/users/me/wishes/{shelterId}", 10L))
                .andExpect(status().isNoContent());
    }

    static class PrincipalResolver implements HandlerMethodArgumentResolver {
        private final Object principal;

        PrincipalResolver(Object principal) {
            this.principal = principal;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && parameter.getParameterType().equals(User.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return principal; // 비로그인 시 null 전달도 가능
        }
    }
}
