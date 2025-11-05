package com.team19.musuimsa.user.service;

import com.team19.musuimsa.config.S3UrlSigner;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.dto.UserPhotoUpdateResponse;
import com.team19.musuimsa.user.dto.UserResponse;
import com.team19.musuimsa.user.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserPhotoService {

    @Value("${aws.s3.base-url}")
    private String s3PublicBaseUrl;

    private final UserService userService;
    private final UserPhotoUploader userPhotoUploader;
    private final S3UrlSigner s3UrlSigner;

    public UserResponse changeMyProfileImage(User loginUser, MultipartFile file) {
        // 1) 업로드
        UserPhotoUpdateResponse uploaded = userPhotoUploader.upload(loginUser.getUserId(), file);
        String newPublicUrl = uploaded.publicUrl();
        String newKey = uploaded.objectKey();

        // 2) 기존 URL 확보
        String oldUrl = safeLoadCurrentUrl(loginUser);

        try {
            // 3) DB 업데이트(정적 URL 저장)
            UserResponse updated = userService.updateUserInfo(
                    new UserUpdateRequest(null, newPublicUrl), loginUser);

            // 4) 커밋 후 이전 이미지 삭제
            registerDeleteOldIfCommitted(oldUrl, updated.profileImageUrl());

            // 5) 응답은 '방금 업로드한 키'로 presign 생성 (URL 파싱 X)
            String signed = s3UrlSigner.signGetUrl(newKey, java.time.Duration.ofMinutes(15));
            return new UserResponse(updated.userId(), updated.email(), updated.nickname(), signed);

        } catch (RuntimeException e) {
            safeDelete(newKey); // 롤백 시 업로드 취소
            throw e;
        }
    }

    private String safeLoadCurrentUrl(User loginUser) {
        try {
            return userService.getUserInfo(loginUser.getUserId()).profileImageUrl();
        } catch (RuntimeException e) {
            return loginUser.getProfileImageUrl();
        }
    }

    private void registerDeleteOldIfCommitted(String oldUrl, String newUrl) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteIfInSameBucket(oldUrl, newUrl);
            return;
        }
        final String oldSnap = oldUrl;
        final String newSnap = newUrl;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteIfInSameBucket(oldSnap, newSnap);
            }
        });
    }

    private void deleteIfInSameBucket(String oldUrl, String newUrl) {
        if (oldUrl == null || oldUrl.isBlank() || oldUrl.equals(newUrl)) {
            return;
        }

        String base = s3PublicBaseUrl.endsWith("/") ? s3PublicBaseUrl : s3PublicBaseUrl + "/";
        if (oldUrl.startsWith(base)) {
            String key = oldUrl.substring(base.length());
            safeDelete(key);
        }
    }

    private void safeDelete(String key) {
        try {
            if (key != null && !key.isBlank()) {
                userPhotoUploader.delete(key);
            }
        } catch (Exception ex) {
            log.warn("Failed to cleanup uploaded image: {}", key, ex);
        }
    }
}

