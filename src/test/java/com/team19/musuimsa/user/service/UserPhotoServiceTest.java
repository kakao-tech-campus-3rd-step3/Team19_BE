package com.team19.musuimsa.user.service;

import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.dto.UserPhotoUpdateResponse;
import com.team19.musuimsa.user.dto.UserResponse;
import com.team19.musuimsa.user.dto.UserUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPhotoServiceTest {

    @Test
    @DisplayName("성공: 같은 버킷이면 DB 갱신 후 이전 객체(S3) 삭제")
    void changeMyProfileImage_success_and_delete_old_when_same_bucket() throws Exception {
        // given
        UserService userService = mock(UserService.class);
        UserPhotoUploader uploader = mock(UserPhotoUploader.class);
        UserPhotoService service = new UserPhotoService(userService, uploader);
        // s3PublicBaseUrl 주입
        setField(service, "s3PublicBaseUrl", "https://cdn.example.com");

        Long userId = 1L;
        User loginUser = new User();
        setField(loginUser, "userId", userId);

        // before: 기존 사진이 같은 버킷 URL
        UserResponse before = new UserResponse(userId, "u@e.com", "nick",
                "https://cdn.example.com/users/1/old.jpg");
        UserPhotoUpdateResponse uploaded = new UserPhotoUpdateResponse(
                "users/1/new.jpg",
                "https://cdn.example.com/users/1/new.jpg",
                "image/jpeg",
                123L
        );
        UserResponse after = new UserResponse(userId, "u@e.com", "nick",
                uploaded.publicUrl());

        when(userService.getUserInfo(userId)).thenReturn(before);
        when(uploader.upload(eq(userId), any(MultipartFile.class))).thenReturn(uploaded);
        when(userService.updateUserInfo(any(UserUpdateRequest.class), eq(loginUser)))
                .thenReturn(after);

        MultipartFile file = mock(MultipartFile.class);

        // when
        UserResponse response = service.changeMyProfileImage(loginUser, file);

        // then
        assertThat(response.profileImageUrl()).isEqualTo(after.profileImageUrl());

        // 이전 키 삭제 확인
        verify(uploader).delete("users/1/old.jpg");

        // 업데이트 요청이 URL만 바꾸는지 확인(닉네임 null)
        ArgumentCaptor<UserUpdateRequest> reqCap = ArgumentCaptor.forClass(UserUpdateRequest.class);
        verify(userService).updateUserInfo(reqCap.capture(), eq(loginUser));
        assertThat(reqCap.getValue().nickname()).isNull();
        assertThat(reqCap.getValue().profileImageUrl()).isEqualTo(uploaded.publicUrl());
    }

    @Test
    @DisplayName("성공: 다른 버킷이면 이전 객체 삭제하지 않음")
    void changeMyProfileImage_success_and_keep_old_when_different_bucket() throws Exception {
        // given
        UserService userService = mock(UserService.class);
        UserPhotoUploader uploader = mock(UserPhotoUploader.class);
        UserPhotoService service = new UserPhotoService(userService, uploader);
        setField(service, "s3PublicBaseUrl", "https://cdn.example.com");

        Long userId = 1L;
        User loginUser = new User();
        setField(loginUser, "userId", userId);

        UserResponse before = new UserResponse(userId, "u@e.com", "nick",
                "https://other-cdn.example.com/users/1/old.jpg"); // 다른 버킷
        UserPhotoUpdateResponse uploaded = new UserPhotoUpdateResponse(
                "users/1/new.jpg",
                "https://cdn.example.com/users/1/new.jpg",
                "image/jpeg",
                123L
        );
        UserResponse after = new UserResponse(userId, "u@e.com", "nick",
                uploaded.publicUrl());

        when(userService.getUserInfo(userId)).thenReturn(before);
        when(uploader.upload(eq(userId), any(MultipartFile.class))).thenReturn(uploaded);
        when(userService.updateUserInfo(any(UserUpdateRequest.class), eq(loginUser)))
                .thenReturn(after);

        MultipartFile file = mock(MultipartFile.class);

        // when
        UserResponse res = service.changeMyProfileImage(loginUser, file);

        // then
        assertThat(res.profileImageUrl()).isEqualTo(after.profileImageUrl());
        verify(uploader, never()).delete("users/1/old.jpg");
    }

    @Test
    @DisplayName("실패: DB 갱신 중 예외면, 방금 업로드한 객체 삭제(롤백 보상)")
    void changeMyProfileImage_fail_then_delete_uploaded_object() throws Exception {
        // given
        UserService userService = mock(UserService.class);
        UserPhotoUploader uploader = mock(UserPhotoUploader.class);
        UserPhotoService service = new UserPhotoService(userService, uploader);
        setField(service, "s3PublicBaseUrl", "https://cdn.example.com");

        Long userId = 1L;
        User loginUser = new User();
        setField(loginUser, "userId", userId);

        UserResponse before = new UserResponse(userId, "u@e.com", "nick",
                "https://cdn.example.com/users/1/old.jpg");
        UserPhotoUpdateResponse uploaded = new UserPhotoUpdateResponse(
                "users/1/new.jpg",
                "https://cdn.example.com/users/1/new.jpg",
                "image/jpeg",
                123L
        );

        when(userService.getUserInfo(userId)).thenReturn(before);
        when(uploader.upload(eq(userId), any(MultipartFile.class))).thenReturn(uploaded);
        when(userService.updateUserInfo(any(UserUpdateRequest.class), eq(loginUser)))
                .thenThrow(new RuntimeException("DB fail"));

        MultipartFile file = mock(MultipartFile.class);

        // when / then
        assertThatThrownBy(() -> service.changeMyProfileImage(loginUser, file))
                .hasMessageContaining("DB fail");

        // 업로드했던 새 객체 제거 시도
        verify(uploader).delete("users/1/new.jpg");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
