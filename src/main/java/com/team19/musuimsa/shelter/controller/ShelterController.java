package com.team19.musuimsa.shelter.controller;

import com.team19.musuimsa.exception.dto.ErrorResponseDto;
import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.ShelterResponse;
import com.team19.musuimsa.shelter.dto.map.MapBoundsRequest;
import com.team19.musuimsa.shelter.dto.map.MapResponse;
import com.team19.musuimsa.shelter.service.ShelterMapService;
import com.team19.musuimsa.shelter.service.ShelterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "쉼터 API", description = "무더위 쉼터 정보 조회 관련 API")
@RestController
@RequestMapping("/api/shelters")
@RequiredArgsConstructor
public class ShelterController {

    private final ShelterService shelterService;

    private final ShelterMapService shelterMapService;

    // 쉼터 메인 페이지 바운딩박스 기반 조회
    @Operation(summary = "지도 범위 내 쉼터/클러스터 조회",
            description =
                    "지도 화면의 현재 보이는 영역(Bounding Box)과 확대 레벨(Zoom)에 따라 쉼터 목록 또는 클러스터 정보를 반환합니다. "
                            + "낮은 Zoom 레벨(12 이하)에서는 클러스터링된 결과가, 높은 Zoom 레벨(13 이상)에서는 개별 쉼터 정보가 반환됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MapResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터 값 (위도/경도 범위 오류 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping
    public ResponseEntity<MapResponse> getByBbox(
            @Parameter(description = "최소 위도 (남서쪽)", example = "37.5", required = true)
            @RequestParam double minLat,
            @Parameter(description = "최소 경도 (남서쪽)", example = "127.0", required = true)
            @RequestParam double minLng,
            @Parameter(description = "최대 위도 (북동쪽)", example = "37.6", required = true)
            @RequestParam double maxLat,
            @Parameter(description = "최대 경도 (북동쪽)", example = "127.1", required = true)
            @RequestParam double maxLng,
            @Parameter(description = "지도 확대 레벨", example = "14", required = true)
            @RequestParam int zoom,
            @Parameter(description = "페이지 번호 (0부터 시작, summary/detail 레벨에서 유효)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기 (기본값 200, 최대 500, summary/detail 레벨에서 유효)",
                    example = "100")
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(shelterMapService.getByBbox(
                new MapBoundsRequest(minLat, minLng, maxLat, maxLng, zoom, page, size)));
    }

    // 가까운 쉼터 조회
    @Operation(summary = "가까운 쉼터 목록 조회",
            description = "현재 위치(위도, 경도)를 기준으로 반경 1km 내의 가까운 쉼터 목록을 거리순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = NearbyShelterResponse.class)))),
            @ApiResponse(responseCode = "400", description = "잘못된 위도/경도 값",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyShelterResponse>> findNearbyShelters(
            @Parameter(description = "현재 위도", example = "37.5665", required = true)
            @RequestParam double latitude,
            @Parameter(description = "현재 경도", example = "126.9780", required = true)
            @RequestParam double longitude
    ) {
        return ResponseEntity.ok(shelterService.findNearbyShelters(latitude, longitude));
    }

    // 쉼터 상세 조회
    @Operation(summary = "쉼터 상세 정보 조회",
            description = "특정 쉼터의 상세 정보를 조회합니다. 현재 위치를 함께 제공하면 쉼터까지의 거리를 계산하여 포함합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ShelterResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 위도/경도 값",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 쉼터를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping("/{shelterId}")
    public ResponseEntity<ShelterResponse> getShelter(
            @Parameter(description = "조회할 쉼터의 ID", example = "1", required = true)
            @PathVariable Long shelterId,
            @Parameter(description = "현재 위도 (거리 계산용)", example = "37.5665", required = true)
            @RequestParam double latitude,
            @Parameter(description = "현재 경도 (거리 계산용)", example = "126.9780", required = true)
            @RequestParam double longitude
    ) {
        return ResponseEntity.ok(shelterService.getShelter(shelterId, latitude, longitude));
    }
}
