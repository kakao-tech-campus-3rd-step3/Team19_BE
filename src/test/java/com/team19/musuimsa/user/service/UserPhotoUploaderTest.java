package com.team19.musuimsa.user.service;

import com.team19.musuimsa.exception.invalid.UnsupportedImageTypeException;
import com.team19.musuimsa.s3.UserPhotoUploader;
import com.team19.musuimsa.user.dto.UserPhotoUpdateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPhotoUploaderTest {

    @Test
    @DisplayName("허용되지 않은 MIME 타입이면 예외")
    void reject_unsupported_mime() throws Exception {
        S3Client s3 = Mockito.mock(S3Client.class);
        UserPhotoUploader uploader = new UserPhotoUploader(s3);
        // 파일 안에서 바로 리플렉션으로 프로퍼티 세팅
        setField(uploader, "bucketName", "bucket");
        setField(uploader, "s3PublicBaseUrl", "https://cdn.example.com");

        MockMultipartFile bad = new MockMultipartFile("file", "a.gif", "image/gif", "x".getBytes());

        assertThatThrownBy(() -> uploader.upload(1L, bad))
                .isInstanceOf(UnsupportedImageTypeException.class);
    }

    @Test
    @DisplayName("jpeg|jpg는 모두 .jpg 확장자로 저장")
    void jpeg_and_jpg_become_jpg() throws Exception {
        S3Client s3 = Mockito.mock(S3Client.class);
        UserPhotoUploader uploader = new UserPhotoUploader(s3);
        setField(uploader, "bucketName", "bucket");
        setField(uploader, "s3PublicBaseUrl", "https://cdn.example.com");

        MockMultipartFile f1 = new MockMultipartFile("file", "x.jpeg", "image/jpeg", "x".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("file", "x.jpg", "image/jpg", "x".getBytes());

        UserPhotoUpdateResponse r1 = uploader.upload(1L, f1);
        UserPhotoUpdateResponse r2 = uploader.upload(1L, f2);

        assertThat(r1.objectKey()).endsWith(".jpg");
        assertThat(r2.objectKey()).endsWith(".jpg");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
