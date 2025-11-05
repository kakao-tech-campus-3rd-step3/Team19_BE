package com.team19.musuimsa.user.service;

import com.team19.musuimsa.config.S3UrlSigner;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.dto.UserPhotoUpdateResponse;
import com.team19.musuimsa.user.dto.UserResponse;
import com.team19.musuimsa.user.dto.UserUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserPhotoServiceTest {

    @Test
    @DisplayName("성공: 같은 버킷이면 DB 갱신 후 이전 객체(S3) 삭제, 응답은 presigned URL")
    void changeMyProfileImage_success_and_delete_old_when_same_bucket() throws Exception {
        UserService userService = mock(UserService.class);
        UserPhotoUploader uploader = mock(UserPhotoUploader.class);
        S3UrlSigner signer = mock(S3UrlSigner.class);

        UserPhotoService service = new UserPhotoService(userService, uploader, signer);
        setField(service, "s3PublicBaseUrl", "https://cdn.example.com/");

        Long userId = 1L;
        User loginUser = new User();
        setField(loginUser, "userId", userId);
        setField(loginUser, "profileImageUrl", "https://cdn.example.com/users/1/old.jpg");

        UserResponse before = new UserResponse(userId, "u@e.com", "nick",
                "https://cdn.example.com/users/1/old.jpg");
        UserPhotoUpdateResponse uploaded = new UserPhotoUpdateResponse(
                "users/1/new.jpg", "https://cdn.example.com/users/1/new.jpg", "image/jpeg", 123L);

        UserResponse afterSaved = new UserResponse(userId, "u@e.com", "nick", uploaded.publicUrl());

        when(userService.getUserInfo(userId)).thenReturn(before);
        when(uploader.upload(eq(userId), any(MultipartFile.class))).thenReturn(uploaded);
        when(userService.updateUserInfo(any(UserUpdateRequest.class), eq(loginUser))).thenReturn(afterSaved);
        when(signer.signGetUrl(eq("users/1/new.jpg"), any(java.time.Duration.class))).thenReturn("https://signed.example.com/new?x=1");

        MultipartFile file = mock(MultipartFile.class);

        beginTx();
        UserResponse res;
        try {
            res = service.changeMyProfileImage(loginUser, file);
        } finally {
            commitTx();
        }

        assertThat(res.profileImageUrl()).isEqualTo("https://signed.example.com/new?x=1");

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        verify(uploader).delete(keyCap.capture());
        assertThat(keyCap.getValue()).isEqualTo("users/1/old.jpg");

        verify(signer).signGetUrl(eq("users/1/new.jpg"), any(Duration.class));
    }

    @Test
    @DisplayName("성공: 다른 버킷이면 이전 객체 삭제하지 않음, 응답은 presigned URL")
    void changeMyProfileImage_success_and_keep_old_when_different_bucket() throws Exception {
        UserService userService = mock(UserService.class);
        UserPhotoUploader uploader = mock(UserPhotoUploader.class);
        S3UrlSigner signer = mock(S3UrlSigner.class);

        UserPhotoService service = new UserPhotoService(userService, uploader, signer);
        setField(service, "s3PublicBaseUrl", "https://cdn.example.com");

        Long userId = 1L;
        User loginUser = new User();
        setField(loginUser, "userId", userId);
        setField(loginUser, "profileImageUrl", "https://external-legacy.com/profile/old.jpg");

        UserResponse before = new UserResponse(userId, "u@e.com", "nick",
                "https://other-cdn.example.com/users/1/old.jpg");
        UserPhotoUpdateResponse uploaded = new UserPhotoUpdateResponse(
                "users/1/new.jpg", "https://cdn.example.com/users/1/new.jpg", "image/jpeg", 123L);
        UserResponse afterSaved = new UserResponse(userId, "u@e.com", "nick", uploaded.publicUrl());

        when(userService.getUserInfo(userId)).thenReturn(before);
        when(uploader.upload(eq(userId), any(MultipartFile.class))).thenReturn(uploaded);
        when(userService.updateUserInfo(any(UserUpdateRequest.class), eq(loginUser))).thenReturn(afterSaved);
        when(signer.signGetUrl(eq("users/1/new.jpg"), any(java.time.Duration.class))).thenReturn("https://signed.example.com/new?x=2");

        MultipartFile file = mock(MultipartFile.class);

        beginTx();
        UserResponse res = service.changeMyProfileImage(loginUser, file);
        commitTx();

        assertThat(res.profileImageUrl()).isEqualTo("https://signed.example.com/new?x=2");
        verify(uploader, never()).delete("users/1/old.jpg"); // 다른 버킷 → 삭제 안 함
        verify(signer).signGetUrl(eq("users/1/new.jpg"), any(Duration.class));
    }

    @Test
    @DisplayName("실패: DB 갱신 중 예외면, 방금 업로드한 객체 삭제(롤백 보상)")
    void changeMyProfileImage_fail_then_delete_uploaded_object() throws Exception {
        UserService userService = mock(UserService.class);
        UserPhotoUploader uploader = mock(UserPhotoUploader.class);

        S3UrlSigner signer = mock(S3UrlSigner.class);

        UserPhotoService service = new UserPhotoService(userService, uploader, signer);
        setField(service, "s3PublicBaseUrl", "https://cdn.example.com");

        Long userId = 1L;
        User loginUser = new User();
        setField(loginUser, "userId", userId);

        UserResponse before = new UserResponse(userId, "u@e.com", "nick",
                "https://cdn.example.com/users/1/old.jpg");
        UserPhotoUpdateResponse uploaded = new UserPhotoUpdateResponse(
                "users/1/new.jpg", "https://cdn.example.com/users/1/new.jpg", "image/jpeg", 123L);

        when(userService.getUserInfo(userId)).thenReturn(before);
        when(uploader.upload(eq(userId), any(MultipartFile.class))).thenReturn(uploaded);
        when(userService.updateUserInfo(any(UserUpdateRequest.class), eq(loginUser)))
                .thenThrow(new RuntimeException("DB fail"));

        MultipartFile file = mock(MultipartFile.class);

        assertThatThrownBy(() -> service.changeMyProfileImage(loginUser, file))
                .hasMessageContaining("DB fail");

        verify(uploader).delete("users/1/new.jpg");
        verifyNoInteractions(signer);
    }

    private static void beginTx() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        TransactionSynchronizationManager.setCurrentTransactionName("testTx");
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
    }

    private static void commitTx() {
        // 스냅샷을 떠서 afterCommit 호출
        var syncs = List.copyOf(TransactionSynchronizationManager.getSynchronizations());
        for (var s : syncs) {
            s.afterCommit();
        }
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
        TransactionSynchronizationManager.setCurrentTransactionName(null);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
