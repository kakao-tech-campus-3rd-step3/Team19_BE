package com.team19.musuimsa.user.service;

import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.dto.UserPhotoUpdateResponse;
import com.team19.musuimsa.user.dto.UserResponse;
import com.team19.musuimsa.user.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserPhotoService {

    @Value("${aws.s3.base-url}")
    private String s3PublicBaseUrl;

    private final UserService userService;
    private final UserPhotoUploader userPhotoUploader;

    @Transactional
    public UserResponse changeMyProfileImage(User loginUser, MultipartFile file) {
        UserResponse before = userService.getUserInfo(loginUser.getUserId());
        UserPhotoUpdateResponse uploaded = userPhotoUploader.upload(loginUser.getUserId(), file);

        try {
            UserResponse updated = userService.updateUserInfo(
                    new UserUpdateRequest(null, uploaded.publicUrl()), loginUser);

            if (before.profileImageUrl() != null
                    && !before.profileImageUrl().isBlank()
                    && !before.profileImageUrl().equals(updated.profileImageUrl())) {
                deleteIfInSameBucket(before.profileImageUrl());
            }
            return updated;
        } catch (RuntimeException e) {
            try {
                userPhotoUploader.delete(uploaded.objectKey());
            } catch (Exception ignore) {
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

