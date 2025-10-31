package com.team19.musuimsa.shelter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자: 쉼터 사진 수동 업데이트 결과")
public record ShelterPhotoUrlUpdateResponse(
        @Schema(description = "업데이트 성공 여부", example = "true")
        boolean updated,
        @Schema(description = "쉼터 사진 URL", example = "https://example.com/shelter/photo.jpg")
        String photoUrl
) {

}
