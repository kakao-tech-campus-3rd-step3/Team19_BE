package com.team19.musuimsa.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team19.musuimsa.exception.auth.InvalidPasswordException;
import com.team19.musuimsa.exception.auth.InvalidRefreshTokenException;
import com.team19.musuimsa.exception.auth.LoginFailedException;
import com.team19.musuimsa.exception.conflict.EmailDuplicateException;
import com.team19.musuimsa.exception.conflict.NicknameDuplicateException;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.dto.LoginRequestDto;
import com.team19.musuimsa.user.dto.SignUpRequestDto;
import com.team19.musuimsa.user.dto.TokenResponseDto;
import com.team19.musuimsa.user.dto.UserPasswordUpdateRequestDto;
import com.team19.musuimsa.user.dto.UserResponseDto;
import com.team19.musuimsa.user.dto.UserUpdateRequestDto;
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
            SignUpRequestDto requestDto = new SignUpRequestDto("test@example.com", "password123",
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
            SignUpRequestDto requestDto = new SignUpRequestDto("test@example.com", "password123",
                    "newuser", "");
            given(userRepository.findByEmail(requestDto.email())).willReturn(Optional.of(user));

            assertThrows(EmailDuplicateException.class, () -> userService.signUp(requestDto));
        }

        @Test
        @DisplayName("실패 - 닉네임 중복")
        void signUp_Fail_NicknameDuplicated() {
            SignUpRequestDto requestDto = new SignUpRequestDto("new@example.com", "password123",
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
            LoginRequestDto requestDto = new LoginRequestDto("test@example.com", "password123");
            String fullRefreshToken = "Bearer refreshToken";

            given(userRepository.findByEmail(requestDto.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(requestDto.password(), user.getPassword())).willReturn(
                    true);
            given(jwtUtil.createAccessToken(user.getEmail())).willReturn("accessToken");
            given(jwtUtil.createRefreshToken(user.getEmail())).willReturn(fullRefreshToken);

            TokenResponseDto tokenResponseDto = userService.login(requestDto);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(tokenResponseDto.accessToken()).isEqualTo("accessToken");
                softly.assertThat(tokenResponseDto.refreshToken()).isEqualTo(fullRefreshToken);
                softly.assertThat(user.getRefreshToken().getToken()).isEqualTo(fullRefreshToken);
            });
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 이메일")
        void login_Fail_UserNotFound() {
            LoginRequestDto requestDto = new LoginRequestDto("wrong@example.com", "password123");
            given(userRepository.findByEmail(requestDto.email())).willReturn(Optional.empty());

            assertThrows(LoginFailedException.class, () -> userService.login(requestDto));
        }

        @Test
        @DisplayName("실패 - 비밀번호 불일치")
        void login_Fail_PasswordMismatch() {
            LoginRequestDto requestDto = new LoginRequestDto("test@example.com", "wrongpassword");
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
            String oldRefreshToken = "Bearer oldRefreshToken";
            String newRefreshToken = "Bearer newRefreshToken";
            String newAccessToken = "newAccessToken";
            String pureToken = "oldRefreshToken";

            user.updateRefreshToken(oldRefreshToken);
            Claims claims = Jwts.claims().subject(user.getEmail()).build();

            given(jwtUtil.validateToken(pureToken)).willReturn(true);
            given(jwtUtil.getUserInfoFromToken(pureToken)).willReturn(claims);
            given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
            given(jwtUtil.createAccessToken(user.getEmail())).willReturn(newAccessToken);
            given(jwtUtil.createRefreshToken(user.getEmail())).willReturn(newRefreshToken);

            TokenResponseDto tokenResponseDto = userService.reissueToken(oldRefreshToken);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(tokenResponseDto.accessToken()).isEqualTo(newAccessToken);
                softly.assertThat(tokenResponseDto.refreshToken()).isEqualTo(newRefreshToken);
                softly.assertThat(user.getRefreshToken().getToken()).isEqualTo(newRefreshToken);
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
        @DisplayName("성공")
        void updateUserInfo_Success() {
            UserUpdateRequestDto requestDto = new UserUpdateRequestDto("newNickname",
                    "newProfile.jpg");
            given(userRepository.findByNickname(requestDto.nickname())).willReturn(
                    Optional.empty());

            UserResponseDto responseDto = userService.updateUserInfo(requestDto,
                    user);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(responseDto.nickname()).isEqualTo("newNickname");
                softly.assertThat(responseDto.profileImageUrl()).isEqualTo("newProfile.jpg");
            });
        }

        @Test
        @DisplayName("실패 - 닉네임 중복")
        void updateUserInfo_Fail_NicknameDuplicated() {
            UserUpdateRequestDto requestDto = new UserUpdateRequestDto("newNickname",
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
            UserPasswordUpdateRequestDto requestDto = new UserPasswordUpdateRequestDto(
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
            UserPasswordUpdateRequestDto requestDto = new UserPasswordUpdateRequestDto(
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