package com.team19.musuimsa.batch;

import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import com.team19.musuimsa.shelter.service.ShelterOpenApiClient;
import com.team19.musuimsa.shelter.service.ShelterPhotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShelterUpdateJobListener implements JobExecutionListener {

    private final ShelterOpenApiClient shelterOpenApiClient;
    private final ShelterPhotoService shelterPhotoService;

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

        int processed = 0, updated = 0, failed = 0;
        if (updatedIds != null) {
            processed = updatedIds.size();
            for (Long id : updatedIds) {
                try {
                    if (shelterPhotoService.updatePhoto(id)) {
                        updated++;
                    }
                } catch (Exception e) {
                    failed++;
                    log.warn("Photo update failed for shelter {}", id, e);
                }
            }
        }

        log.info("<<<< Shelter Update Job END (photo) processed={}, updated={}, failed={}", processed, updated, failed);
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

}