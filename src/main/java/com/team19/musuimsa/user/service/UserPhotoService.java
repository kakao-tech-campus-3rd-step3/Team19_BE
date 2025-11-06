package com.team19.musuimsa.user.service;

import com.team19.musuimsa.s3.S3FileUploader;
import com.team19.musuimsa.s3.S3UrlSigner;
import com.team19.musuimsa.s3.dto.S3UploadResponse;
import com.team19.musuimsa.user.domain.User;
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
    private final S3FileUploader s3FileUploader;
    private final S3UrlSigner s3UrlSigner;

    private static final String USER_PREFIX = "users";

    public UserResponse changeMyProfileImage(User loginUser, MultipartFile file) {
        // 1) 업로드
        String prefix = USER_PREFIX + "/" + loginUser.getUserId();
        S3UploadResponse uploaded = s3FileUploader.upload(prefix, file);
        // DB에는 반드시 정적 URL 저장 (쿼리 제거)
        String newPublicUrl = stripQuery(uploaded.publicUrl());
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
        final String oldSnap = oldUrl;
        final String newSnap = newUrl;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteIfInSameBucket(oldSnap, newSnap);
            }
        });
    }

    private String safeLoadCurrentUrl(User loginUser) {
        return loginUser.getProfileImageUrl();
    }

    private void registerDeleteOldIfCommitted(String oldUrl, String newUrl) {
        final String oldKey = toKeyOrNull(oldUrl);
        final String newKey = toKeyOrNull(newUrl);
        Runnable task = () -> deleteIfDifferentManagedKey(oldKey, newKey);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private void deleteIfDifferentManagedKey(String oldKey, String newKey) {
        if (oldKey == null || oldKey.isBlank()) {
            return;
        }
        if (oldKey.equals(newKey)) {
            return; // 동일 키면 삭제 X
        }
        safeDelete(oldKey);
    }

    private void safeDelete(String key) {
        try {
            if (key != null && !key.isBlank()) {
                s3FileUploader.delete(key);
            }
        } catch (Exception ex) {
            log.warn("Failed to cleanup uploaded image: {}", key, ex);
        }
    }

    // 조회 DTO의 정적 URL → presigned로 교체 시 사용
    public UserResponse signIfPresent(UserResponse dto) {
        String key = toKeyOrNull(dto.profileImageUrl());
        if (key == null || key.isBlank()) {
            return dto;
        }
        String signed = s3UrlSigner.signGetUrl(key, java.time.Duration.ofMinutes(15));
        return new UserResponse(dto.userId(), dto.email(), dto.nickname(), signed);
    }

    private String stripQuery(String url) {
        if (url == null) {
            return null;
        }
        int i = url.indexOf('?');
        return (i >= 0) ? url.substring(0, i) : url;
    }

    private String toKeyOrNull(String url) {
        // 1. 입력 유효성 검사
        if (url == null || url.isBlank()) {
            return null;
        }

        // 2. Base URL 기반 파싱
        String base = s3PublicBaseUrl.endsWith("/") ? s3PublicBaseUrl : s3PublicBaseUrl + "/";

        if (url.startsWith(base)) {
            String keyWithQuery = url.substring(base.length());

            int i = keyWithQuery.indexOf('?');
            return (i >= 0) ? keyWithQuery.substring(0, i) : keyWithQuery;
        }

        // 3. AWS 호스트 기반 파싱
        int at = url.indexOf(".amazonaws.com/");
        if (at > 0) {
            int start = at + ".amazonaws.com/".length();

            int q = url.indexOf('?', start);
            return (q > 0) ? url.substring(start, q) : url.substring(start);
        }

        // 4. 추출 실패
        return null;
    }
}

