package com.team19.musuimsa.review.controller;

import com.team19.musuimsa.exception.dto.ErrorResponseDto;
import com.team19.musuimsa.review.dto.CreateReviewRequest;
import com.team19.musuimsa.review.dto.ReviewResponse;
import com.team19.musuimsa.review.dto.UpdateReviewRequest;
import com.team19.musuimsa.review.service.ReviewService;
import com.team19.musuimsa.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "리뷰 API", description = "쉼터 리뷰 작성, 수정, 삭제, 조회 관련 API")
@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // 리뷰 작성
    @Operation(summary = "리뷰 작성", description = "특정 쉼터에 대한 리뷰를 작성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "리뷰 작성 성공 (헤더 Location에 리뷰 리소스 URI 포함)",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (유효성 검사 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "유효성 검사 실패",
                                    value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"rating: 1에서 5 사이여야 합니다, content: 리뷰는 100자까지 작성 가능합니다.\", \"path\": \"/api/shelters/1/reviews\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "인증 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"회원가입 또는 로그인 후 이용 가능합니다. \", \"path\": \"/api/shelters/1/reviews\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 쉼터를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "쉼터 없음",
                                    value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"해당 ID의 쉼터를 찾을 수 없습니다: 999\", \"path\": \"/api/shelters/999/reviews\"}"))),
            @ApiResponse(responseCode = "409", description = "동시성 문제 발생 (리뷰 집계 업데이트 충돌)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "동시성 충돌",
                                    value = "{\"status\": 409, \"error\": \"Conflict\", \"message\": \"잠시 후 다시 시도해 주세요.\", \"path\": \"/api/shelters/1/reviews\"}")))
    })
    @PostMapping("/shelters/{shelterId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @Parameter(description = "리뷰 생성 정보", required = true,
                    schema = @Schema(implementation = CreateReviewRequest.class))
            @Valid @RequestBody CreateReviewRequest request,

            @Parameter(description = "리뷰를 작성할 쉼터의 ID", example = "1", required = true)
            @PathVariable Long shelterId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user) {

        ReviewResponse response = reviewService.createReview(shelterId, request, user);

        URI location = URI.create("/api/reviews/" + response.reviewId());

        return ResponseEntity.created(location).body(response);
    }

    // 리뷰 수정
    @Operation(summary = "리뷰 수정", description = "자신이 작성한 리뷰의 내용, 별점, 사진 URL을 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "리뷰 수정 성공",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (유효성 검사 실패)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "유효성 검사 실패",
                                    value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"rating: 1에서 5 사이여야 합니다\", \"path\": \"/api/reviews/1\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "인증 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"회원가입 또는 로그인 후 이용 가능합니다. \", \"path\": \"/api/reviews/1\"}"))),
            @ApiResponse(responseCode = "403", description = "자신의 리뷰만 수정 가능 (권한 없음)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "권한 없음",
                                    value = "{\"status\": 403, \"error\": \"Forbidden\", \"message\": \"본인의 리뷰에만 접근할 수 있습니다.\", \"path\": \"/api/reviews/1\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 리뷰를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "리뷰 없음",
                                    value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"해당 ID의 리뷰를 찾을 수 없습니다: 999\", \"path\": \"/api/reviews/999\"}"))),
            @ApiResponse(responseCode = "409", description = "동시성 문제 발생",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "동시성 충돌",
                                    value = "{\"status\": 409, \"error\": \"Conflict\", \"message\": \"잠시 후 다시 시도해 주세요.\", \"path\": \"/api/reviews/1\"}")))
    })
    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @Parameter(description = "수정할 리뷰의 ID", example = "1", required = true)
            @PathVariable Long reviewId,
            @Parameter(description = "수정할 리뷰 정보 (변경할 필드만 포함)", required = true,
                    schema = @Schema(implementation = UpdateReviewRequest.class))
            @Valid @RequestBody UpdateReviewRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user) {

        ReviewResponse response = reviewService.updateReview(reviewId, request, user);

        return ResponseEntity.ok(response);
    }

    // 리뷰 삭제
    @Operation(summary = "리뷰 삭제", description = "자신이 작성한 리뷰를 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "리뷰 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "인증 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"회원가입 또는 로그인 후 이용 가능합니다. \", \"path\": \"/api/reviews/1\"}"))),
            @ApiResponse(responseCode = "403", description = "자신의 리뷰만 삭제 가능 (권한 없음)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "권한 없음",
                                    value = "{\"status\": 403, \"error\": \"Forbidden\", \"message\": \"본인의 리뷰에만 접근할 수 있습니다.\", \"path\": \"/api/reviews/1\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 리뷰를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "리뷰 없음",
                                    value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"해당 ID의 리뷰를 찾을 수 없습니다: 999\", \"path\": \"/api/reviews/999\"}"))),
            @ApiResponse(responseCode = "409", description = "동시성 문제 발생",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "동시성 충돌",
                                    value = "{\"status\": 409, \"error\": \"Conflict\", \"message\": \"잠시 후 다시 시도해 주세요.\", \"path\": \"/api/reviews/1\"}")))
    })
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "삭제할 리뷰의 ID", example = "1", required = true)
            @PathVariable Long reviewId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user) {

        reviewService.deleteReview(reviewId, user);

        return ResponseEntity.noContent().build();
    }

    // 리뷰 단건 조회
    @Operation(summary = "리뷰 단건 조회", description = "특정 리뷰 ID로 리뷰 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 리뷰를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "리뷰 없음",
                                    value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"해당 ID의 리뷰를 찾을 수 없습니다: 999\", \"path\": \"/api/reviews/999\"}")))
    })
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(
            @Parameter(description = "조회할 리뷰의 ID", example = "1", required = true)
            @PathVariable Long reviewId) {

        ReviewResponse response = reviewService.getReview(reviewId);

        return ResponseEntity.ok(response);
    }

    // 쉼터 리뷰 조회
    @Operation(summary = "쉼터별 리뷰 목록 조회", description = "특정 쉼터에 작성된 모든 리뷰를 최신순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = ReviewResponse.class)))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 쉼터를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "쉼터 없음",
                                    value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"해당 ID의 쉼터를 찾을 수 없습니다: 999\", \"path\": \"/api/shelters/999/reviews\"}")))
    })
    @GetMapping("/shelters/{shelterId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviewByShelter(
            @Parameter(description = "리뷰를 조회할 쉼터의 ID", example = "1", required = true)
            @PathVariable Long shelterId) {

        List<ReviewResponse> reviews = reviewService.getReviewsByShelter(shelterId);

        return ResponseEntity.ok(reviews);
    }

    // 내가 쓴 리뷰 조회
    @Operation(summary = "내가 쓴 리뷰 목록 조회", description = "로그인된 사용자가 작성한 모든 리뷰를 최신순으로 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = ReviewResponse.class)))), // 배열 응답 명시
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "인증 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"회원가입 또는 로그인 후 이용 가능합니다. \", \"path\": \"/api/users/me/reviews\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "사용자 없음",
                                    value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"해당 ID의 사용자를 찾을 수 없습니다: 1\", \"path\": \"/api/users/me/reviews\"}")))
    })
    @GetMapping("/users/me/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviewByUser(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user) {

        List<ReviewResponse> reviews = reviewService.getReviewsByUser(user);

        return ResponseEntity.ok(reviews);
    }
}
