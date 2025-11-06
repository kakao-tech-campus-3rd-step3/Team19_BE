package com.team19.musuimsa.review.service;

import com.team19.musuimsa.review.domain.Review;
import com.team19.musuimsa.review.dto.ReviewResponse;
import com.team19.musuimsa.s3.S3FileUploader;
import com.team19.musuimsa.s3.S3UrlSigner;
import com.team19.musuimsa.s3.dto.S3UploadResponse;
import com.team19.musuimsa.user.domain.User;
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

class ReviewPhotoServiceTest {

    private static final String REVIEW_PREFIX_KEY = "reviews/100";
    private static final Long REVIEW_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final String S3_BASE_URL = "https://cdn.example.com/";

    @Test
    @DisplayName("성공: 같은 버킷이면 DB 갱신 후 이전 객체(S3) 삭제, 응답은 presigned URL")
    void uploadReviewImage_success_and_delete_old() throws Exception {
        // Given
        ReviewService reviewService = mock(ReviewService.class);
        S3FileUploader uploader = mock(S3FileUploader.class);
        S3UrlSigner signer = mock(S3UrlSigner.class);
        ReviewPhotoService service = new ReviewPhotoService(reviewService, uploader, signer);
        setField(service, "s3PublicBaseUrl", S3_BASE_URL);

        User loginUser = new User("user@e.com", "pw", "nick", "profile.url");
        setField(loginUser, "userId", USER_ID);

        Review reviewEntity = new Review();
        setField(reviewEntity, "reviewId", REVIEW_ID);
        setField(reviewEntity, "user", loginUser);

        setField(reviewEntity, "photoUrl", S3_BASE_URL + "reviews/100/old.jpg");

        String signedUrl = "https://signed.example.com/new?x=1";

        setupMocks(reviewService, uploader, signer,
                S3_BASE_URL + "reviews/100/old.jpg", "reviews/100/new.jpg",
                S3_BASE_URL + "reviews/100/new.jpg", signedUrl,
                loginUser, reviewEntity);

        MultipartFile file = mock(MultipartFile.class);

        // When
        beginTx();
        ReviewResponse res;
        try {
            res = service.uploadReviewImage(REVIEW_ID, file, loginUser);
        } finally {
            commitTx();
        }

        // Then
        assertThat(res.photoUrl()).isEqualTo(signedUrl);

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        verify(uploader).delete(keyCap.capture());
        assertThat(keyCap.getValue()).isEqualTo("reviews/100/old.jpg");

        verify(signer).signGetUrl(eq("reviews/100/new.jpg"), any(Duration.class));
    }

    @Test
    @DisplayName("성공: 다른 버킷이면 이전 객체 삭제하지 않음, 응답은 presigned URL")
    void uploadReviewImage_success_and_keep_old_when_different_bucket() throws Exception {
        // Given
        ReviewService reviewService = mock(ReviewService.class);
        S3FileUploader uploader = mock(S3FileUploader.class);
        S3UrlSigner signer = mock(S3UrlSigner.class);
        ReviewPhotoService service = new ReviewPhotoService(reviewService, uploader, signer);
        setField(service, "s3PublicBaseUrl", S3_BASE_URL);

        User loginUser = new User("user@e.com", "pw", "nick", "profile.url");
        setField(loginUser, "userId", USER_ID);

        Review reviewEntity = new Review();
        setField(reviewEntity, "reviewId", REVIEW_ID);
        setField(reviewEntity, "user", loginUser);

        setField(reviewEntity, "photoUrl", "https://external-cdn.com/reviews/old.jpg");

        String signedUrl = "https://signed.example.com/new?x=2";

        setupMocks(reviewService, uploader, signer,
                "https://external-cdn.com/reviews/old.jpg", "reviews/100/new.jpg",
                S3_BASE_URL + "reviews/100/new.jpg", signedUrl,
                loginUser, reviewEntity);

        MultipartFile file = mock(MultipartFile.class);

        // When
        beginTx();
        ReviewResponse res;
        try {
            res = service.uploadReviewImage(REVIEW_ID, file, loginUser);
        } finally {
            commitTx();
        }

        // Then
        assertThat(res.photoUrl()).isEqualTo(signedUrl);

        verify(uploader, never()).delete(any(String.class));
        verify(signer).signGetUrl(eq("reviews/100/new.jpg"), any(Duration.class));
    }

    @Test
    @DisplayName("실패: DB 갱신 중 예외면, 방금 업로드한 객체 삭제(롤백 보상)")
    void uploadReviewImage_fail_then_delete_uploaded_object() throws Exception {
        // Given
        ReviewService reviewService = mock(ReviewService.class);
        S3FileUploader uploader = mock(S3FileUploader.class);
        S3UrlSigner signer = mock(S3UrlSigner.class);
        ReviewPhotoService service = new ReviewPhotoService(reviewService, uploader, signer);
        setField(service, "s3PublicBaseUrl", S3_BASE_URL);

        User loginUser = new User("user@e.com", "pw", "nick", "profile.url");
        setField(loginUser, "userId", USER_ID);

        Review reviewEntity = new Review();
        setField(reviewEntity, "reviewId", REVIEW_ID);
        setField(reviewEntity, "user", loginUser);

        setField(reviewEntity, "photoUrl", S3_BASE_URL + "reviews/100/old.jpg");

        S3UploadResponse uploaded = new S3UploadResponse(
                "reviews/100/new.jpg", S3_BASE_URL + "reviews/100/new.jpg", "image/jpeg", 123L);

        when(reviewService.getReviewEntity(eq(REVIEW_ID))).thenReturn(reviewEntity);
        when(uploader.upload(eq(REVIEW_PREFIX_KEY), any(MultipartFile.class))).thenReturn(uploaded);
        when(reviewService.updateReviewPhotoUrl(eq(REVIEW_ID), any(String.class), eq(loginUser)))
                .thenThrow(new RuntimeException("DB fail: Transaction rolled back"));

        MultipartFile file = mock(MultipartFile.class);

        // When & Then
        assertThatThrownBy(() -> service.uploadReviewImage(REVIEW_ID, file, loginUser))
                .hasMessageContaining("DB fail");

        verify(uploader).delete("reviews/100/new.jpg");
        verifyNoInteractions(signer);
    }

    private static void beginTx() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionName("testTx");
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
    }

    private static void commitTx() {
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

    private ReviewResponse setupMocks(
            ReviewService reviewService, S3FileUploader uploader, S3UrlSigner signer,
            String oldUrl, String newKey, String newPublicUrl, String signedUrl,
            User loginUser, Review reviewEntity) {

        when(reviewService.getReviewEntity(eq(REVIEW_ID))).thenReturn(reviewEntity);

        S3UploadResponse uploaded = new S3UploadResponse(
                newKey, newPublicUrl, "image/jpeg", 123L);
        when(uploader.upload(eq(REVIEW_PREFIX_KEY), any(MultipartFile.class))).thenReturn(uploaded);

        ReviewResponse afterSaved = new ReviewResponse(
                REVIEW_ID, 10L, "쉼터", USER_ID, "닉", "내용", 5,
                newPublicUrl, "프로필URL", null, null); // withPhotoUrl 제거

        when(reviewService.updateReviewPhotoUrl(eq(REVIEW_ID), any(String.class), eq(loginUser)))
                .thenReturn(afterSaved);

        when(signer.signGetUrl(eq(newKey), any(Duration.class))).thenReturn(signedUrl);

        return afterSaved;
    }
}