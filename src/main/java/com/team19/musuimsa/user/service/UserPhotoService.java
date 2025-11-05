package com.team19.musuimsa.user.service;

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

    public UserResponse changeMyProfileImage(User loginUser, MultipartFile file) {
        UserPhotoUpdateResponse uploaded = userPhotoUploader.upload(loginUser.getUserId(), file);

        String oldUrl = null;
        try {
            UserResponse current = userService.getUserInfo(loginUser.getUserId());
            oldUrl = current.profileImageUrl();
        } catch (RuntimeException e) {
            log.warn("Failed to load current user info. oldUrl fallback to loginUser field.", e);
            oldUrl = loginUser.getProfileImageUrl();
        }

        try {
            UserResponse updated = userService.updateUserInfo(
                    new UserUpdateRequest(null, uploaded.publicUrl()), loginUser);

            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                final String oldUrlSnapshot = oldUrl;
                final String newUrlSnapshot = updated.profileImageUrl();

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (oldUrlSnapshot != null && !oldUrlSnapshot.isBlank()
                                && !oldUrlSnapshot.equals(newUrlSnapshot)) {
                            deleteIfInSameBucket(oldUrlSnapshot);
                        }
                    }
                });
            } else {
                // 트랜잭션 없으면 즉시 처리
                if (oldUrl != null && !oldUrl.isBlank()
                        && !oldUrl.equals(updated.profileImageUrl())) {
                    deleteIfInSameBucket(oldUrl);
                }
            }
            return updated;
        } catch (RuntimeException e) {
            try {
                userPhotoUploader.delete(uploaded.objectKey());
            } catch (Exception deleteEx) {
                log.warn("Failed to cleanup uploaded image: {}", uploaded.objectKey(), deleteEx);
            }
            throw e;
        }
    }

    private void deleteIfInSameBucket(String oldUrl) {
        if (oldUrl == null || oldUrl.isBlank()) {
            return;
        }

        String base = s3PublicBaseUrl.endsWith("/") ? s3PublicBaseUrl : s3PublicBaseUrl + "/";
        if (oldUrl.startsWith(base)) {
            String key = oldUrl.substring(base.length());
            userPhotoUploader.delete(key);
        }
    }
}

