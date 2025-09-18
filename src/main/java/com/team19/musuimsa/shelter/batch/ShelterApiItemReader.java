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

    @Override
    public ExternalShelterItem read() {
        // 현재 페이지의 아이템을 다 읽었거나, 처음 시작하는 경우 데이터를 가져옴
        if (items == null || nextIndex >= items.size()) {
            ExternalResponse response = shelterOpenApiClient.fetchPage(page++);

            // API 응답이 없거나, body가 비어있으면 더 이상 읽을 데이터가 없으므로 null을 반환하여 종료 신호를 보냄
            if (response == null || response.body() == null || response.body().isEmpty()) {
                items = null;
                return null;
            }

            // 새로운 데이터로 교체하고, 인덱스를 초기화
            items = response.body();
            nextIndex = 0;
        }

        // 현재 인덱스의 아이템을 반환하고, 인덱스를 증가시킴
        return items.get(nextIndex++);
    }
}
