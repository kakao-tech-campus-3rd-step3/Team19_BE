package com.team19.musuimsa.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.team19.musuimsa.exception.auth.AuthenticationException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@example.com", "password123", "testUser", "profile.jpg");
        ReflectionTestUtils.setField(user, "userId", 1L);
    }

    @Test
    @DisplayName("리프레시 토큰을 성공적으로 업데이트한다.")
    void updateRefreshToken() {
        String newRefreshToken = "new-refresh-token";

        user.updateRefreshToken(newRefreshToken);

        assertThat(user.getRefreshToken().getToken()).isEqualTo(newRefreshToken);
    }

    @Test
    @DisplayName("리프레시 토큰을 무효화(null로 변경)한다.")
    void invalidateRefreshToken() {
        user.updateRefreshToken("existing-token");

        user.invalidateRefreshToken();

        assertThat(user.getRefreshToken()).isNull();
    }

    @Nested
    @DisplayName("사용자 정보 수정(updateUser) 테스트")
    class UpdateUserTest {

        @Test
        @DisplayName("닉네임과 프로필 이미지 URL을 모두 성공적으로 변경한다.")
        void updateUser_AllFields() {
            String newNickname = "newNickname";
            String newProfileImageUrl = "newProfile.jpg";

            user.updateUser(newNickname, newProfileImageUrl);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getNickname()).isEqualTo(newNickname);
                softly.assertThat(user.getProfileImageUrl()).isEqualTo(newProfileImageUrl);
            });
        }

        @Test
        @DisplayName("닉네임만 성공적으로 변경한다.")
        void updateUser_OnlyNickname() {
            String newNickname = "newNickname";
            String originalProfileImageUrl = user.getProfileImageUrl();

            user.updateUser(newNickname, null);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getNickname()).isEqualTo(newNickname);
                softly.assertThat(user.getProfileImageUrl()).isEqualTo(originalProfileImageUrl);
            });
        }

        @Test
        @DisplayName("프로필 이미지 URL만 성공적으로 변경한다.")
        void updateUser_OnlyProfileImageUrl() {
            String newProfileImageUrl = "newProfile.jpg";
            String originalNickname = user.getNickname();

            user.updateUser(null, newProfileImageUrl);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getNickname()).isEqualTo(originalNickname);
                softly.assertThat(user.getProfileImageUrl()).isEqualTo(newProfileImageUrl);
            });
        }

        @Test
        @DisplayName("null 또는 공백 값으로는 닉네임과 프로필이 변경되지 않는다.")
        void updateUser_WithNullAndBlankValues() {
            String originalNickname = user.getNickname();
            String originalProfileImageUrl = user.getProfileImageUrl();

            user.updateUser(null, null);
            user.updateUser("", " ");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getNickname()).isEqualTo(originalNickname);
                softly.assertThat(user.getProfileImageUrl()).isEqualTo(originalProfileImageUrl);
            });
        }
    }

    @Test
    @DisplayName("비밀번호를 성공적으로 변경한다.")
    void updatePassword() {
        String newPassword = "newPassword123!";

        user.updatePassword(newPassword);

        assertThat(user.getPassword()).isEqualTo(newPassword);
    }

    @Nested
    @DisplayName("사용자 권한 검증(validateUserPermission) 테스트")
    class ValidateUserPermissionTest {

        @Test
        @DisplayName("성공 - 동일한 사용자일 경우 예외가 발생하지 않는다.")
        void validateUserPermission_Success() {
            User sameUser = new User("test@example.com", "password", "testUser", "p.jpg");
            ReflectionTestUtils.setField(sameUser, "userId", 1L);

            assertDoesNotThrow(() -> user.validateUserPermission(sameUser, "에러 메시지"));
        }

        @Test
        @DisplayName("실패 - 다른 사용자일 경우 AuthenticationException 예외가 발생한다.")
        void validateUserPermission_Fail() {
            User otherUser = new User("other@example.com", "password", "other", "p.jpg");
            ReflectionTestUtils.setField(otherUser, "userId", 2L);
            String errorMessage = "자신의 정보만 수정할 수 있습니다.";

            assertThatThrownBy(() -> user.validateUserPermission(otherUser, errorMessage))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage(errorMessage);
        }
    }
}