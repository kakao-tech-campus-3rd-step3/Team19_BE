package com.team19.musuimsa.s3;

import com.team19.musuimsa.exception.external.S3UploadException;
import com.team19.musuimsa.exception.invalid.InvalidFileException;
import com.team19.musuimsa.exception.invalid.UnsupportedImageTypeException;
import com.team19.musuimsa.s3.dto.S3UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3FileUploader {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024;

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.base-url}")
    private String s3PublicBaseUrl;

    public S3UploadResponse upload(String prefix, MultipartFile multipartFile) {

        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new InvalidFileException("업로드할 파일이 비어있습니다.");
        }

        if (multipartFile.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidFileException(
                    "파일 크기가 너무 큽니다. 최대 " + (MAX_SIZE_BYTES / 1024 / 1024) + "MB까지 업로드 가능합니다.");
        }

        String contentType = multipartFile.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType.toLowerCase())) {
            throw new UnsupportedImageTypeException(contentType);
        }

        // 2. 파일명 및 Object Key 생성
        String fileExtension = resolveFileExtension(multipartFile.getOriginalFilename(), contentType);
        String randomId = UUID.randomUUID().toString().replace("-", "");
        String objectKey = prefix + "/" + randomId + "." + fileExtension;

        // 3. PutObjectRequest 빌드
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000")
                .build();

        // 4. S3 업로드 실행
        try (InputStream inputStream = multipartFile.getInputStream()) {
            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, multipartFile.getSize()));
        } catch (IOException e) {
            throw new S3UploadException(bucketName, objectKey, e);
        }

        // 5. Public URL 생성
        String publicUrl = s3PublicBaseUrl.endsWith("/")
                ? s3PublicBaseUrl + objectKey
                : s3PublicBaseUrl + "/" + objectKey;

        return new S3UploadResponse(objectKey, publicUrl, contentType, multipartFile.getSize());
    }

    private String resolveFileExtension(String originalFilename, String contentType) {
        if ("image/jpeg".equalsIgnoreCase(contentType) || "image/jpg".equalsIgnoreCase(contentType)) {
            return "jpg";
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return "png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return "webp";
        }

        // Fallback: MIME 타입으로 결정할 수 없을 경우 파일명에서 확장자 추출
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }

        return "bin";
    }

    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        s3Client.deleteObject(b -> b.bucket(bucketName).key(objectKey));
    }
}