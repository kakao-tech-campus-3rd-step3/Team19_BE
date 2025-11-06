package com.team19.musuimsa.review.service;

import com.team19.musuimsa.review.domain.Review;
import com.team19.musuimsa.review.dto.ReviewResponse;
import com.team19.musuimsa.s3.S3FileUploader;
import com.team19.musuimsa.s3.S3UrlSigner;
import com.team19.musuimsa.s3.dto.S3UploadResponse;
import com.team19.musuimsa.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewPhotoService {

    @Value("${aws.s3.base-url}")
    private String s3PublicBaseUrl;

    private final ReviewService reviewService;
    private final S3FileUploader s3FileUploader;
    private final S3UrlSigner s3UrlSigner;

    private static final String REVIEW_PREFIX = "reviews";

    public ReviewResponse uploadReviewImage(Long reviewId, MultipartFile file, User loginUser) {

        Review review = reviewService.getReviewEntity(reviewId);
        review.assertOwnedBy(loginUser);

        // 1. 파일 업로드 및 Key, URL 확보
        String prefix = REVIEW_PREFIX + "/" + reviewId;
        S3UploadResponse uploaded = s3FileUploader.upload(prefix, file);

        String newPublicUrl = stripQuery(uploaded.publicUrl());
        String newKey = uploaded.objectKey();

        String oldUrl = review.getPhotoUrl();

        try {
            // 2. DB 업데이트 (ReviewService의 트랜잭션 내에서 처리)
            ReviewResponse updated = reviewService.updateReviewPhotoUrl(reviewId, newPublicUrl, loginUser);

            // 3. 트랜잭션 커밋 성공 후 이전 이미지 삭제
            registerDeleteOldIfCommitted(oldUrl, updated.photoUrl());

            // 4. 응답은 '방금 업로드한 키'로 presign 생성
            String signed = s3UrlSigner.signGetUrl(newKey, Duration.ofMinutes(15));

            // photoUrl만 Presigned URL로 교체하여 반환
            return updated.withPhotoUrl(signed);

        } catch (RuntimeException e) {
            safeDelete(newKey);
            throw e;
        }
    }

    public ReviewResponse signReviewPhotoIfPresent(ReviewResponse dto) {
        String key = toKeyOrNull(dto.photoUrl());
        if (key == null || key.isBlank()) {
            return dto;
        }
        String signed = s3UrlSigner.signGetUrl(key, Duration.ofMinutes(15));
        return dto.withProfileImageUrl(signed);
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

    private String stripQuery(String url) {
        if (url == null) {
            return null;
        }
        int i = url.indexOf('?');
        return (i >= 0) ? url.substring(0, i) : url;
    }

    private String toKeyOrNull(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String base = s3PublicBaseUrl.endsWith("/") ? s3PublicBaseUrl : s3PublicBaseUrl + "/";
        if (url.startsWith(base)) {
            String keyWithQuery = url.substring(base.length());

            int i = keyWithQuery.indexOf('?');
            return (i >= 0) ? keyWithQuery.substring(0, i) : keyWithQuery;
        }

        int at = url.indexOf(".amazonaws.com/");
        if (at > 0) {
            int start = at + ".amazonaws.com/".length();

            int q = url.indexOf('?', start);
            return (q > 0) ? url.substring(start, q) : url.substring(start);
        }

        return null;
    }
}