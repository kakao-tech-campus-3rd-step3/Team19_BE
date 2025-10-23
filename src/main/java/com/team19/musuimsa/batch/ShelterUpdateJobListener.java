package com.team19.musuimsa.batch;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.shelter.service.ShelterOpenApiClient;
import com.team19.musuimsa.shelter.service.ShelterPhotoService;
import com.team19.musuimsa.shelter.util.GeoHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShelterUpdateJobListener implements JobExecutionListener {

    private final ShelterOpenApiClient shelterOpenApiClient;
    private final ShelterPhotoService shelterPhotoService;

    private final ShelterRepository shelterRepository;
    private final CacheManager cacheManager;
    private final Optional<StringRedisTemplate> redisTemplate;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(">>>> Shelter Update Job START");
        Map<Long, ExternalShelterItem> externalShelterData = fetchAllExternalShelterData();

        jobExecution.getExecutionContext().put("externalShelterData", externalShelterData);

        log.info(">>>> Fetched {} items from external API.", externalShelterData.size());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        ExecutionContext ctx = jobExecution.getExecutionContext();
        Set<Long> updatedIds = (Set<Long>) ctx.get(ShelterImportBatchConfig.LOCATION_UPDATED_IDS_KEY);

        if (updatedIds == null || updatedIds.isEmpty()) {
            log.info("<<<< Shelter Update Job END (변경된 쉼터 없음, 사진 갱신 생략)");
            return;
        }

        int processed = 0;
        int updated = 0;
        int failed = 0;

        for (Long id : updatedIds) {
            try {
                if (shelterPhotoService.updatePhoto(id)) {
                    updated++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Photo update failed for shelter {}", id, e);
            }
            processed++;
        }

        log.info("<<<< Shelter Update Job END (photo) processed={}, updated={}, failed={}", processed, updated, failed);

        // 2) 선택 캐시 무효화
        try {
            invalidateSheltersMapCacheByIds(updatedIds);
        } catch (Exception e) {
            // 문제가 생기면 안전하게 캐시 전체 무효화
            log.warn("Selective cache invalidation failed. Fallback to full clear.", e);
            Cache cache = cacheManager.getCache("sheltersMap");
            if (cache != null) {
                cache.clear();
            }
        }
        log.info("<<<< Shelter Update Job END");
    }

    private Map<Long, ExternalShelterItem> fetchAllExternalShelterData() {
        List<ExternalShelterItem> allItems = new ArrayList<>();
        int page = 1;
        while (true) {
            ExternalResponse response = shelterOpenApiClient.fetchPage(page);
            if (response == null || response.body() == null || response.body().isEmpty()) {
                break;
            }
            allItems.addAll(response.body());

            int total = response.totalCount() == null ? 0 : response.totalCount();
            int rows = response.numOfRows() == null ? 0 : response.numOfRows();
            int lastPage = (rows > 0) ? (int) Math.ceil(total / (double) rows) : page;

            if (page >= lastPage) {
                break;
            }
            page++;
        }
        return allItems.stream()
                .collect(Collectors.toMap(ExternalShelterItem::rstrFcltyNo, Function.identity()));
    }

    // 변경된 쉼터 id들의 좌표로부터, 해당될 수 있는 z 레벨의 geohash 셀 prefix를 계산해 패턴 삭제
    private void invalidateSheltersMapCacheByIds(Set<Long> ids) {
        List<Shelter> shelters = shelterRepository.findAllById(ids);
        if (shelters.isEmpty()) {
            return;
        }

        // 서비스 분기에서 사용중인 줌 레벨 대역
        int[] zooms = {12, 13, 14, 15, 16, 17};

        // prod(redis): 패턴 삭제, dev(caffeine)면 clear 폴백
        if (redisTemplate.isPresent()) {
            StringRedisTemplate redis = redisTemplate.get();
            int deletedSum = 0;

            for (Shelter s : shelters) {
                BigDecimal lat = s.getLatitude();
                BigDecimal lng = s.getLongitude();
                if (lat == null || lng == null) {
                    continue;
                }

                for (int z : zooms) {
                    int precision = GeoHashUtil.prefixForZoom(z);
                    String cell = GeoHashUtil.snapBbox(lat, lng, lat, lng, precision);
                    String pattern = "musuimsa::sheltersMap::v1:z" + z + ":gh:" + cell + ":*";
                    deletedSum += scanAndDelete(redis, pattern);
                }
            }
            log.info("Selective cache invalidation done. deletedKeys={}", deletedSum);
        } else {
            // dev(caffeine) — 패턴 삭제 수단이 없어 캐시 전체 clear
            Cache cache = cacheManager.getCache("sheltersMap");
            if (cache != null) {
                cache.clear();
            }
            log.info("Caffeine cache -> sheltersMap cleared (dev fallback).");
        }
    }

    // Redis에서 패턴으로 SCAN 후 DEL
    private int scanAndDelete(StringRedisTemplate redis, String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
        Set<String> keys = new HashSet<>();
        try (Cursor<byte[]> cursor = redis.getConnectionFactory()
                .getConnection().scan(options)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
        } catch (Exception e) {
            log.warn("SCAN failed for pattern {}", pattern, e);
        }

        if (!keys.isEmpty()) {
            redis.delete(keys);
        }
        return keys.size();
    }
}