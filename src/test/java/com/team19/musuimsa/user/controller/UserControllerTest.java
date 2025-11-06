package com.team19.musuimsa.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team19.musuimsa.exception.handler.GlobalExceptionHandler;
import com.team19.musuimsa.security.UserDetailsImpl;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.dto.LoginRequest;
import com.team19.musuimsa.user.dto.SignUpRequest;
import com.team19.musuimsa.user.dto.TokenResponse;
import com.team19.musuimsa.user.dto.UserPasswordUpdateRequest;
import com.team19.musuimsa.user.dto.UserResponse;
import com.team19.musuimsa.user.dto.UserUpdateRequest;
import com.team19.musuimsa.user.service.UserPhotoService;
import com.team19.musuimsa.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Mock
    private UserPhotoService userPhotoService;

    private UserDetailsImpl userDetails;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "password123", "testUser", "profile.jpg");
        userDetails = new UserDetailsImpl(testUser);

        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new MockAuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jacksonConverter,
                        new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .build();
    }

    public class MockAuthenticationPrincipalArgumentResolver implements
            HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(UserDetailsImpl.class) &&
                    parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return userDetails;
        }
    }

    @Nested
    @DisplayName("회원가입 API 테스트")
    class SignUpTest {

        @Test
        @DisplayName("성공")
        void signUp_Success() throws Exception {
            SignUpRequest requestDto = new SignUpRequest("test@example.com", "password123",
                    "nickname", "");
            given(userService.signUp(any(SignUpRequest.class))).willReturn(1L);

            mockMvc.perform(post("/api/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/users/1"))
                    .andExpect(jsonPath("$").value("회원가입이 성공적으로 완료되었습니다."));
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 이메일 형식")
        void signUp_Fail_InvalidEmail() throws Exception {
            SignUpRequest requestDto = new SignUpRequest("test", "password123",
                    "nickname", "");

            mockMvc.perform(post("/api/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 비밀번호가 8자 미만")
        void signUp_Fail_PasswordTooShort() throws Exception {
            SignUpRequest requestDto = new SignUpRequest("test@example.com", "pass",
                    "nickname", "");

            mockMvc.perform(post("/api/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 비밀번호가 20자 초과")
        void signUp_Fail_PasswordTooLong() throws Exception {
            SignUpRequest requestDto = new SignUpRequest("test@example.com",
                    "password1234567890123", "nickname", "");

            mockMvc.perform(post("/api/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 닉네임이 비어있음")
        void signUp_Fail_BlankNickname() throws Exception {
            SignUpRequest requestDto = new SignUpRequest("test@example.com", "password123",
                    "", "");

            mockMvc.perform(post("/api/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());
        }
    }


    @Nested
    @DisplayName("로그인 API 테스트")
    class LoginTest {

        @Test
        @DisplayName("성공")
        void login_Success() throws Exception {
            LoginRequest requestDto = new LoginRequest("test@example.com", "password");
            TokenResponse tokenResponse = new TokenResponse("access-token",
                    "refresh-token");
            given(userService.login(any(LoginRequest.class))).willReturn(tokenResponse);

            mockMvc.perform(post("/api/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
        }

        @Test
        @DisplayName("실패 - 이메일이 비어있음")
        void login_Fail_BlankEmail() throws Exception {
            LoginRequest requestDto = new LoginRequest("", "password");

            mockMvc.perform(post("/api/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 이메일 형식")
        void login_Fail_InvalidEmail() throws Exception {
            LoginRequest requestDto = new LoginRequest("test", "password");

            mockMvc.perform(post("/api/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 비밀번호가 비어있음")
        void login_Fail_BlankPassword() throws Exception {
            LoginRequest requestDto = new LoginRequest("test@example.com", "");

            mockMvc.perform(post("/api/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());
        }
    }


    @Test
    @DisplayName("로그아웃 API 테스트")
    void logoutUser() throws Exception {
        doNothing().when(userService).logout(any(User.class));

        mockMvc.perform(post("/api/users/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("성공적으로 로그아웃 되었습니다."));
    }

    @Test
    @DisplayName("토큰 재발급 API 테스트")
    void reissueTokens() throws Exception {
        TokenResponse tokenResponse = new TokenResponse("new-access-token",
                "new-refresh-token");
        given(userService.reissueToken(any(String.class))).willReturn(tokenResponse);

        mockMvc.perform(post("/api/users/reissue")
                        .header("Authorization-Refresh", "Bearer refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("회원 정보 수정 API 테스트")
    void updateUserInfo() throws Exception {
        UserUpdateRequest requestDto = new UserUpdateRequest("newNickname", "newProfile.jpg");
        UserResponse updatedUser = new UserResponse(1L, "test@example.com", "newNickname",
                "newProfile.jpg");
        given(userService.updateUserInfo(any(UserUpdateRequest.class),
                any(User.class))).willReturn(updatedUser);

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("newNickname"))
                .andExpect(jsonPath("$.profileImageUrl").value("newProfile.jpg"));
    }

    @Nested
    @DisplayName("비밀번호 변경 API 테스트")
    class UpdateUserPasswordTest {

        @Test
        @DisplayName("성공")
        void updateUserPassword_Success() throws Exception {
            UserPasswordUpdateRequest requestDto = new UserPasswordUpdateRequest(
                    "currentPassword", "newPassword");
            doNothing().when(userService)
                    .updateUserPassword(any(UserPasswordUpdateRequest.class),
                            any(User.class));

            mockMvc.perform(patch("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value("비밀번호가 성공적으로 변경되었습니다."));
        }

        @Test
        @DisplayName("실패 - 현재 비밀번호가 비어있음")
        void updateUserPassword_Fail_BlankCurrentPassword() throws Exception {
            UserPasswordUpdateRequest requestDto = new UserPasswordUpdateRequest("",
                    "newPassword");

            mockMvc.perform(patch("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 새 비밀번호가 비어있음")
        void updateUserPassword_Fail_BlankNewPassword() throws Exception {
            UserPasswordUpdateRequest requestDto = new UserPasswordUpdateRequest(
                    "currentPassword", "");

            mockMvc.perform(patch("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());
        }
    }


    @Test
    @DisplayName("회원 탈퇴 API 테스트")
    void deleteUser() throws Exception {
        doNothing().when(userService).deleteUser(any(User.class));

        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isNoContent());
    }
}
