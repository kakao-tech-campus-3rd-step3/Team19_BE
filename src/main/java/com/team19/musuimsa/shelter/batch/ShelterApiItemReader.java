package com.team19.musuimsa.shelter.batch;

import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import com.team19.musuimsa.shelter.service.ShelterOpenApiClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;

@RequiredArgsConstructor
public class ShelterApiItemReader implements ItemReader<ExternalShelterItem> {

    private final ShelterOpenApiClient shelterOpenApiClient;
    private int page = 1;
    private List<ExternalShelterItem> items;
    private int nextIndex = 0;
    private boolean isFinished = false;

    @Override
    public ExternalShelterItem read() {
        if (isFinished) {
            return null;
        }

        if (items == null || nextIndex >= items.size()) {
            ExternalResponse response = shelterOpenApiClient.fetchPage(page++);
            if (response == null || response.body() == null || response.body().isEmpty()) {
                isFinished = true;
                return null;
            }
            items = response.body();
            nextIndex = 0;
        }

        ExternalShelterItem nextItem = null;
        if (nextIndex < items.size()) {
            nextItem = items.get(nextIndex++);
        } else {
            isFinished = true;
        }

        return nextItem;
    }
}
