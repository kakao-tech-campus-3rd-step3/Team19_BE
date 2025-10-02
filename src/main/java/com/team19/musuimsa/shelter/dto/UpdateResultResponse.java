package com.team19.musuimsa.shelter.dto;

public record UpdateResultResponse(
        boolean isChanged, // DB 저장을 위한 변경 여부
        boolean locationChanged // 사진 갱신 트리거 여부 (위/경도 변경 시)
) {
}
