package com.team19.musuimsa.wish.controller;

import com.team19.musuimsa.exception.dto.ErrorResponseDto;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.wish.dto.CreateWishResponse;
import com.team19.musuimsa.wish.dto.WishListResponse;
import com.team19.musuimsa.wish.service.WishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "위시리스트 API", description = "사용자 위시리스트(찜) 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/wishes")
public class WishController {

    private final WishService wishService;

    // 위시 추가
    @Operation(summary = "위시리스트 추가", description = "특정 쉼터를 위시리스트에 추가합니다. 이미 추가된 경우 기존 정보를 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "위시 추가 성공 (헤더 Location에 위시 리소스 URI 포함)", content = @Content(
                    schema = @Schema(implementation = CreateWishResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 쉼터를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping("/{shelterId}")
    public ResponseEntity<CreateWishResponse> createWish(
            @Parameter(description = "위시리스트에 추가할 쉼터의 ID", example = "1", required = true)
            @PathVariable Long shelterId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    ) {
        CreateWishResponse response = wishService.createWish(shelterId, user);

        URI location = URI.create("/api/users/me/wishes/" + response.wishId());
        return ResponseEntity.created(location).body(response);
    }

    // 위시 조회
    @Operation(summary = "위시리스트 조회",
            description = "로그인된 사용자의 위시리스트 목록을 조회합니다. 현재 위치를 제공하면 각 쉼터까지의 거리를 계산하여 포함합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = WishListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<WishListResponse> getWishes(
            @Parameter(description = "현재 위도 (거리 계산용, 선택)", example = "37.5665")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "현재 경도 (거리 계산용, 선택)", example = "126.9780")
            @RequestParam(required = false) Double longitude,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    ) {
        WishListResponse response = wishService.getWishes(user, latitude, longitude);
        return ResponseEntity.ok(response);
    }

    // 위시 삭제
    @Operation(summary = "위시리스트 삭제", description = "특정 쉼터를 위시리스트에서 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/{shelterId}")
    public ResponseEntity<Void> deleteWish(
            @Parameter(description = "위시리스트에서 삭제할 쉼터의 ID", example = "1", required = true)
            @PathVariable Long shelterId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    ) {
        wishService.deleteWish(shelterId, user);
        return ResponseEntity.noContent().build();
    }
}
