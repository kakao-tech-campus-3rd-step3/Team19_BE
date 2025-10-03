package com.team19.musuimsa.mapillary;

import com.team19.musuimsa.mapillary.dto.MapillaryImageResponse;
import com.team19.musuimsa.mapillary.dto.MapillaryImagesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

@Component
public class MapillaryPhotoAgent {

    private static final Logger log = LoggerFactory.getLogger(MapillaryPhotoAgent.class);

    private final RestClient mapillary;
    private final S3Client s3Client;
    private final String bucket;
    private final String baseUrl;
    private final String folder;
    private final int searchLimit;

    public MapillaryPhotoAgent(
            @Qualifier("mapillaryRestClient") RestClient mapillary,
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.base-url}") String baseUrl,
            @Value("${aws.s3.folder:shelters}") String folder,
            @Value("${mapillary.limit}") int searchLimit
    ) {
        this.mapillary = mapillary;
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.baseUrl = baseUrl;
        this.folder = folder;
        this.searchLimit = searchLimit;
    }

    // lat/lon 기준 반경(m)에서 가장 가까운 Mapillary 사진을 찾아 S3로 업로드 후 공개 URL 반환
    public Optional<String> findAndStore(BigDecimal latitude, BigDecimal longitude, int radiusMeters, long shelterId) {
        // 1) 반경 → BBOX로 변환하여 /images 조회
        String url = buildImagesUrl(latitude.doubleValue(), longitude.doubleValue(), radiusMeters, searchLimit);
        MapillaryImagesResponse res = mapillary.get().uri(url).retrieve().body(MapillaryImagesResponse.class);

        int dataSize = res.data() == null ? 0 : res.data().size();
        log.info("[Mapillary] url={}, dataSize={}", url, dataSize);

        if (res.data() == null || res.data().isEmpty()) {
            // 1차에 없으면 반경 2배로 한 번 더 시도
            String url2 = buildImagesUrl(latitude.doubleValue(), longitude.doubleValue(), radiusMeters * 2, searchLimit);
            res = mapillary.get().uri(url2).retrieve().body(MapillaryImagesResponse.class);
            int size2 = res.data() == null ? 0 : res.data().size();
            log.info("[Mapillary] retry url={}, dataSize={}", url2, size2);
            if (res.data() == null || res.data().isEmpty()) {
                return Optional.empty();
            }
        }

        // 2) 썸네일 있는 것만 남기고 거리 계산해서 가장 가까운 한 장
        Optional<MapillaryImageResponse> pick = res.data().stream()
                .filter(i -> i.bestThumbUrl() != null)
                .min(Comparator.comparingDouble(i -> distanceM(latitude.doubleValue(), longitude.doubleValue(), i.latitude(), i.longitude())));

        if (pick.isEmpty()) {
            return Optional.empty();
        }

        String thumbUrl = pick.get().bestThumbUrl();

        // 3) 썸네일 바이트 다운로드
        ResponseEntity<byte[]> entity = mapillary.get().uri(thumbUrl).retrieve().toEntity(byte[].class);
        entity.getBody();

        // 4) Content-Type → 확장자 결정 (기본 jpg)
        entity.getHeaders().getContentType();
        String contentType = entity.getHeaders().getContentType().toString();
        String ext = contentType.contains("webp") ? ".webp" : ".jpg";

        // 5) S3 업로드
        String key = folder + "/" + shelterId + ext;
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(entity.getBody())
        );

        String publicUrl = baseUrl + "/" + key;
        log.info("[Mapillary] uploaded: {}", publicUrl);
        return Optional.of(publicUrl);
    }

    private static String buildImagesUrl(double lat, double lon, double radiusMeters, int limit) {
        double dLat = radiusMeters / 111_000d;
        double dLon = radiusMeters / (111_000d * Math.cos(Math.toRadians(lat)));

        double minLat = lat - dLat;
        double maxLat = lat + dLat;
        double minLon = lon - dLon;
        double maxLon = lon + dLon;

        String fields = "id,thumb_2048_url,thumb_1024_url,computed_geometry,captured_at";
        return UriComponentsBuilder.fromPath("/images")
                .queryParam("bbox", String.format(Locale.US, "%.6f,%.6f,%.6f,%.6f",
                        minLon, minLat, maxLon, maxLat))
                .queryParam("limit", limit)
                .queryParam("fields", fields)
                .toUriString();
    }

    private static double distanceM(double lat1, double lon1, Double lat2, Double lon2) {
        if (lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }

        double R = 6371000d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
