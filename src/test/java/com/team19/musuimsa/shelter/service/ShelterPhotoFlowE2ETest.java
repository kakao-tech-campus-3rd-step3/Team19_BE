package com.team19.musuimsa.shelter.service;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ShelterPhotoFlowE2ETest {

    static MockWebServer mapillary = new MockWebServer();

    @RegisterExtension
    static S3MockExtension s3 = S3MockExtension.builder()
            .withSecureConnection(false)
            .silent()
            .build();

    @Autowired
    ShelterRepository shelterRepository;

    @Autowired
    ShelterPhotoService shelterPhotoService;

    @Autowired
    S3Client s3Client;

    @BeforeAll
    static void setUp() throws Exception {
        mapillary.start();
    }

    @AfterAll
    static void tearDown() throws Exception {
        mapillary.shutdown();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("aws.s3.region", () -> "ap-northeast-2");
        r.add("aws.s3.bucket", () -> "musuimsa");
        r.add("aws.s3.folder", () -> "shelters");
        r.add("aws.s3.base-url", () -> "https://mock-s3.local");
        r.add("aws.s3.endpoint", () -> s3.getServiceEndpoint());
        r.add("aws.s3.path-style-access", () -> "true");
        r.add("mapillary.api-base", () -> "http://localhost:" + mapillary.getPort());
        r.add("mapillary.access-token", () -> "dummy");
    }

    @Test
    void flow() {
        final String bucket = "musuimsa";
        try {
            s3Client.headBucket(b -> b.bucket(bucket));
        } catch (Exception ignored) {
            s3Client.createBucket(b -> b.bucket(bucket));
        }

        // 1) /images 응답 페이크 (썸네일/좌표 포함)
        long shelterId = 1L;
        double lat = 37.5665;
        double lon = 126.9780;

        String thumbPath = "/thumbs/" + shelterId + ".jpg";
        String thumbAbsUrl = "http://localhost:" + mapillary.getPort() + thumbPath;

        String imagesJson = """
                {
                  "data": [
                    {
                      "id": "IMG_1",
                      "thumb_2048_url": "%s",
                      "computed_geometry": {
                        "type": "Point",
                        "coordinates": [ %f, %f ]
                      },
                      "captured_at": 1700000000000
                    }
                  ]
                }
                """.formatted(thumbAbsUrl, lon, lat);

        mapillary.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .addHeader("Content-Type", "application/json")
                .setBody(imagesJson));

        // 2) 썸네일 바이트 응답 페이크
        byte[] imageBytes = "FAKE_JPEG_BYTES".getBytes(StandardCharsets.UTF_8);
        mapillary.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .addHeader("Content-Type", "image/jpeg")
                .setBody(new Buffer().write(imageBytes)));

        // 3) Shelter 생성 및 저장
        Shelter shelter = Shelter.builder()
                .shelterId(shelterId)
                .name("종로 무더위 쉼터")
                .address("서울 종로구 세종대로 175")
                .latitude(BigDecimal.valueOf(lat))
                .longitude(BigDecimal.valueOf(lon))
                .isOutdoors(false)
                .capacity(50)
                .fanCount(3)
                .airConditionerCount(1)
                .build();
        shelterRepository.saveAndFlush(shelter);

        boolean updated = shelterPhotoService.updatePhoto(shelterId);

        assertTrue(updated, "사진 갱신이 true 여야 함");

        Shelter saved = shelterRepository.findById(shelterId).orElseThrow();
        String expectedUrl = "https://mock-s3.local/shelters/" + shelterId + ".jpg";
        assertEquals(expectedUrl, saved.getPhotoUrl(), "DB photoUrl이 예상 공개 URL이어야 함");

        // S3Mock에 실제 업로드 되었는지 확인
        ResponseBytes<GetObjectResponse> got = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket("musuimsa")
                        .key("shelters/" + shelterId + ".jpg")
                        .build()
        );
        assertArrayEquals(imageBytes, got.asByteArray(), "S3에 업로드된 바이트가 썸네일과 동일해야 함");

        assertEquals(0, mapillary.getRequestCount() - 2, "Mapillary 호출 횟수 확인");
    }
}
