package com.team19.musuimsa.shelter.controller;

import com.team19.musuimsa.exception.dto.ErrorResponseDto;
import com.team19.musuimsa.shelter.dto.BatchUpdateResponse;
import com.team19.musuimsa.shelter.dto.ShelterImportResponse;
import com.team19.musuimsa.shelter.dto.ShelterPhotoUrlUpdateResponse;
import com.team19.musuimsa.shelter.service.ShelterImportService;
import com.team19.musuimsa.shelter.service.ShelterPhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 API", description = "데이터 임포트 및 관리용 API (내부용)")
@RestController
@RequestMapping("/api/admin/shelters")
@RequiredArgsConstructor
public class ShelterAdminController {

    private final ShelterImportService importService;
    private final ShelterPhotoService photoService;

    @Operation(summary = "쉼터 데이터 수동 임포트",
            description = "공공데이터 API로부터 쉼터 데이터를 가져와 DB에 저장/업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "임포트 성공",
                    content = @Content(
                            schema = @Schema(implementation = ShelterImportResponse.class),
                            examples = @ExampleObject(name = "임포트 성공",
                                    value = "{\"saved\": 59639}")
                    )),
            @ApiResponse(responseCode = "502", description = "외부 API 호출 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "공공데이터 API 실패",
                                    value = "{\"status\": 502, \"error\": \"Bad Gateway\", \"message\": \"외부 API 호출 실패: https://www.safetydata.go.kr/...\", \"path\": \"/api/admin/shelters/import\"}")))
    })
    @PostMapping("/import")
    public ResponseEntity<ShelterImportResponse> importShelters() {
        int saved = importService.importOnce();
        return ResponseEntity.ok(new ShelterImportResponse(saved));
    }


    // Shelter photoUrl 단건 저장
    @Operation(summary = "쉼터 사진 단건 업데이트",
            description = "특정 쉼터 ID에 대해 Mapillary API를 조회하여 쉼터 사진을 S3에 저장하고 URL을 DB에 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사진 업데이트 성공",
                    content = @Content(schema = @Schema(
                            implementation = ShelterPhotoUrlUpdateResponse.class),
                            examples = @ExampleObject(name = "사진 업데이트 성공",
                                    value = "{\"updated\": true, \"photoUrl\": \"https://musuimsa.s3.ap-northeast-2.amazonaws.com/shelters/1.jpg\"}")
                    )),
            @ApiResponse(responseCode = "204", description = "해당 쉼터의 사진을 찾지 못함 (업데이트 없음)"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 쉼터를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "쉼터 없음",
                                    value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"해당 ID의 쉼터를 찾을 수 없습니다: 999\", \"path\": \"/api/admin/shelters/photos/999\"}"))),
            @ApiResponse(responseCode = "502", description = "외부(Mapillary/S3) API 호출 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "Mapillary API 실패",
                                    value = "{\"status\": 502, \"error\": \"Bad Gateway\", \"message\": \"외부 API 호출 실패: https://graph.mapillary.com/...\", \"path\": \"/api/admin/shelters/photos/1\"}")))
    })
    @PostMapping("/photos/{shelterId}")
    public ResponseEntity<ShelterPhotoUrlUpdateResponse> updateOne(
            @Parameter(description = "사진을 업데이트할 쉼터 ID", example = "1", required = true)
            @PathVariable Long shelterId) {
        Optional<String> urlOpt = photoService.updatePhotoAndReturnUrl(shelterId);

        if (urlOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(new ShelterPhotoUrlUpdateResponse(true, urlOpt.get()));
    }

    // Shelter photoUrl 모두 저장
    @Operation(summary = "쉼터 사진 전체 업데이트 (배치)",
            description = "사진 URL이 없는 쉼터들을 대상으로 배치를 실행하여 사진을 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "배치 실행 완료",
                    content = @Content(
                            schema = @Schema(implementation = BatchUpdateResponse.class),
                            examples = @ExampleObject(name = "배치 실행 완료",
                                    value = "{\"processed\": 100, \"updated\": 85, \"failed\": 15}")
                    ))
    })
    @PostMapping("/photos/all")
    public ResponseEntity<BatchUpdateResponse> updateBatch(
            @Parameter(description = "페이지당 처리할 쉼터 수", example = "100")
            @RequestParam(defaultValue = "100") int pageSize,
            @Parameter(description = "최대 실행할 페이지 수 (무한 실행 방지)", example = "100")
            @RequestParam(defaultValue = "100") int maxPages
    ) {
        BatchUpdateResponse batchReport = photoService.updateAllMissing(pageSize, maxPages);
        return ResponseEntity.ok(batchReport);
    }
}