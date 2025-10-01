package com.team19.musuimsa.batch;

import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import com.team19.musuimsa.shelter.service.ShelterOpenApiClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShelterUpdateJobListener implements JobExecutionListener {

    private final ShelterOpenApiClient shelterOpenApiClient;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(">>>> Shelter Update Job START");
        Map<Long, ExternalShelterItem> externalShelterData = fetchAllExternalShelterData();

        jobExecution.getExecutionContext().put("externalShelterData", externalShelterData);

        log.info(">>>> Fetched {} items from external API.", externalShelterData.size());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
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

}