package com.team19.musuimsa.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team19.musuimsa.exception.auth.InvalidPasswordException;
import com.team19.musuimsa.exception.auth.InvalidRefreshTokenException;
import com.team19.musuimsa.exception.auth.LoginFailedException;
import com.team19.musuimsa.exception.conflict.EmailDuplicateException;
import com.team19.musuimsa.exception.conflict.NicknameDuplicateException;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.dto.LoginRequest;
import com.team19.musuimsa.user.dto.SignUpRequest;
import com.team19.musuimsa.user.dto.TokenResponse;
import com.team19.musuimsa.user.dto.UserPasswordUpdateRequest;
import com.team19.musuimsa.user.dto.UserResponse;
import com.team19.musuimsa.user.dto.UserUpdateRequest;
import com.team19.musuimsa.user.repository.UserRepository;
import com.team19.musuimsa.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@example.com", "encodedPassword", "testUser", "profile.jpg");
        ReflectionTestUtils.setField(user, "userId", 1L);
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class SignUpTest {

        @Test
        @DisplayName("성공")
        void signUp_Success() {
            SignUpRequest requestDto = new SignUpRequest("test@example.com", "password123",
                    "testUser", "");
            given(userRepository.findByEmail(requestDto.email())).willReturn(Optional.empty());
            given(userRepository.findByNickname(requestDto.nickname())).willReturn(
                    Optional.empty());
            given(passwordEncoder.encode(requestDto.password())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(user);

            Long userId = userService.signUp(requestDto);

            assertThat(userId).isEqualTo(1L);
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("실패 - 이메일 중복")
        void signUp_Fail_EmailDuplicated() {
            SignUpRequest requestDto = new SignUpRequest("test@example.com", "password123",
                    "newuser", "");
            given(userRepository.findByEmail(requestDto.email())).willReturn(Optional.of(user));

            assertThrows(EmailDuplicateException.class, () -> userService.signUp(requestDto));
        }

        @Test
        @DisplayName("실패 - 닉네임 중복")
        void signUp_Fail_NicknameDuplicated() {
            SignUpRequest requestDto = new SignUpRequest("new@example.com", "password123",
                    "testUser", "");
            given(userRepository.findByEmail(requestDto.email())).willReturn(Optional.empty());
            given(userRepository.findByNickname(requestDto.nickname())).willReturn(
                    Optional.of(user));

            assertThrows(NicknameDuplicateException.class, () -> userService.signUp(requestDto));
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Test
        @DisplayName("성공")
        void login_Success() {
            LoginRequest requestDto = new LoginRequest("test@example.com", "password123");
            String fullRefreshToken = "Bearer refreshToken";

            given(userRepository.findByEmail(requestDto.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(requestDto.password(), user.getPassword())).willReturn(
                    true);
            given(jwtUtil.createAccessToken(user.getEmail())).willReturn("accessToken");
            given(jwtUtil.createRefreshToken(user.getEmail())).willReturn(fullRefreshToken);

            TokenResponse tokenResponse = userService.login(requestDto);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(tokenResponse.accessToken()).isEqualTo("accessToken");
                softly.assertThat(tokenResponse.refreshToken()).isEqualTo(fullRefreshToken);
                softly.assertThat(user.getRefreshToken().getToken()).isEqualTo(fullRefreshToken);
            });
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 이메일")
        void login_Fail_UserNotFound() {
            LoginRequest requestDto = new LoginRequest("wrong@example.com", "password123");
            given(userRepository.findByEmail(requestDto.email())).willReturn(Optional.empty());

            assertThrows(LoginFailedException.class, () -> userService.login(requestDto));
        }

        @Test
        @DisplayName("실패 - 비밀번호 불일치")
        void login_Fail_PasswordMismatch() {
            LoginRequest requestDto = new LoginRequest("test@example.com", "wrongpassword");
            given(userRepository.findByEmail(requestDto.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(requestDto.password(), user.getPassword())).willReturn(
                    false);

            assertThrows(LoginFailedException.class, () -> userService.login(requestDto));
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class ReissueTokenTest {

        @Test
        @DisplayName("성공")
        void reissueToken_Success() {
            String oldRefreshTokenWithPrefix = "Bearer oldRefreshToken";
            String oldPureRefreshToken = "oldRefreshToken";
            String newAccessToken = "newAccessToken";
            String newPureRefreshToken = "newRefreshToken";

            user.updateRefreshToken(oldPureRefreshToken);
            Claims claims = Jwts.claims().subject(user.getEmail()).build();

            given(jwtUtil.validateToken(oldPureRefreshToken)).willReturn(true);
            given(jwtUtil.getUserInfoFromToken(oldPureRefreshToken)).willReturn(claims);
            given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
            given(jwtUtil.createAccessToken(user.getEmail())).willReturn(newAccessToken);
            given(jwtUtil.createRefreshToken(user.getEmail())).willReturn(newPureRefreshToken);

            TokenResponse tokenResponse = userService.reissueToken(oldRefreshTokenWithPrefix);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(tokenResponse.accessToken()).isEqualTo(newAccessToken);
                softly.assertThat(tokenResponse.refreshToken()).isEqualTo(newPureRefreshToken);
                softly.assertThat(user.getRefreshToken().getToken()).isEqualTo(newPureRefreshToken);
            });
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 토큰")
        void reissueToken_Fail_InvalidToken() {
            String refreshToken = "Bearer invalidToken";
            String pureToken = "invalidToken";

            given(jwtUtil.validateToken(pureToken)).willReturn(false);

            assertThrows(InvalidRefreshTokenException.class,
                    () -> userService.reissueToken(refreshToken));
        }
    }


    @Nested
    @DisplayName("회원 정보 수정 테스트")
    class UpdateUserInfoTest {

        @Test
        @DisplayName("성공 - 닉네임과 프로필 이미지 모두 변경")
        void updateUserInfo_Success_AllFields() {
            UserUpdateRequest requestDto = new UserUpdateRequest("newNickname",
                    "newProfile.jpg");
            given(userRepository.findByNickname(requestDto.nickname())).willReturn(
                    Optional.empty());

            UserResponse responseDto = userService.updateUserInfo(requestDto,
                    user);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(responseDto.nickname()).isEqualTo("newNickname");
                softly.assertThat(responseDto.profileImageUrl()).isEqualTo("newProfile.jpg");
            });
        }

        @Test
        @DisplayName("성공 - 닉네임만 변경")
        void updateUserInfo_Success_OnlyNickname() {
            UserUpdateRequest requestDto = new UserUpdateRequest("newNickname",
                    user.getProfileImageUrl());
            given(userRepository.findByNickname(requestDto.nickname())).willReturn(
                    Optional.empty());

            UserResponse responseDto = userService.updateUserInfo(requestDto,
                    user);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(responseDto.nickname()).isEqualTo("newNickname");
                softly.assertThat(responseDto.profileImageUrl())
                        .isEqualTo(user.getProfileImageUrl());
            });
        }

        @Test
        @DisplayName("성공 - 프로필 이미지만 변경")
        void updateUserInfo_Success_OnlyProfileImage() {
            UserUpdateRequest requestDto = new UserUpdateRequest(user.getNickname(),
                    "newProfile.jpg");

            UserResponse responseDto = userService.updateUserInfo(requestDto,
                    user);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(responseDto.nickname()).isEqualTo(user.getNickname());
                softly.assertThat(responseDto.profileImageUrl()).isEqualTo("newProfile.jpg");
            });
            verify(userRepository, never()).findByNickname(any());
        }

        @Test
        @DisplayName("성공 - 변경 사항 없음")
        void updateUserInfo_Success_NoChanges() {
            UserUpdateRequest requestDto = new UserUpdateRequest(user.getNickname(),
                    user.getProfileImageUrl());

            UserResponse responseDto = userService.updateUserInfo(requestDto, user);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(responseDto.nickname()).isEqualTo(user.getNickname());
                softly.assertThat(responseDto.profileImageUrl())
                        .isEqualTo(user.getProfileImageUrl());
            });
            verify(userRepository, never()).findByNickname(any());
        }

        @Test
        @DisplayName("성공 - null 또는 빈 값으로 요청 시 기존 정보 유지")
        void updateUserInfo_Success_NullAndEmptyValues() {
            UserUpdateRequest requestDto = new UserUpdateRequest(null, "");

            UserResponse responseDto = userService.updateUserInfo(requestDto, user);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(responseDto.nickname()).isEqualTo(user.getNickname());
                softly.assertThat(responseDto.profileImageUrl())
                        .isEqualTo(user.getProfileImageUrl());
            });
            verify(userRepository, never()).findByNickname(any());
        }

        @Test
        @DisplayName("실패 - 닉네임 중복")
        void updateUserInfo_Fail_NicknameDuplicated() {
            UserUpdateRequest requestDto = new UserUpdateRequest("newNickname",
                    "newProfile.jpg");
            User existingUser = new User("exist@example.com", "password", "newNickname", "p.jpg");
            given(userRepository.findByNickname(requestDto.nickname())).willReturn(
                    Optional.of(existingUser));

            assertThrows(NicknameDuplicateException.class,
                    () -> userService.updateUserInfo(requestDto, user));
        }
    }

    @Nested
    @DisplayName("비밀번호 변경 테스트")
    class UpdateUserPasswordTest {

        @Test
        @DisplayName("성공")
        void updateUserPassword_Success() {
            UserPasswordUpdateRequest requestDto = new UserPasswordUpdateRequest(
                    "encodedPassword", "newPassword");
            given(passwordEncoder.matches(requestDto.currentPassword(),
                    user.getPassword())).willReturn(true);
            given(passwordEncoder.encode(requestDto.newPassword())).willReturn(
                    "newEncodedPassword");

            userService.updateUserPassword(requestDto, user);

            assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
        }

        @Test
        @DisplayName("실패 - 현재 비밀번호 불일치")
        void updateUserPassword_Fail_InvalidPassword() {
            UserPasswordUpdateRequest requestDto = new UserPasswordUpdateRequest(
                    "wrongPassword", "newPassword");
            given(passwordEncoder.matches(requestDto.currentPassword(),
                    user.getPassword())).willReturn(false);

            assertThrows(InvalidPasswordException.class,
                    () -> userService.updateUserPassword(requestDto, user));
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 테스트")
    class DeleteUserTest {

        @Test
        @DisplayName("성공")
        void deleteUser_Success() {
            userService.deleteUser(user);

            verify(userRepository, times(1)).delete(user);
        }
    }
}