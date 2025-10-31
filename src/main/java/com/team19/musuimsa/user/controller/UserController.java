package com.team19.musuimsa.user.controller;

import com.team19.musuimsa.exception.dto.ErrorResponseDto;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.domain.UserDevice;
import com.team19.musuimsa.user.dto.LoginRequest;
import com.team19.musuimsa.user.dto.SignUpRequest;
import com.team19.musuimsa.user.dto.TokenResponse;
import com.team19.musuimsa.user.dto.UserDeviceRegisterRequest;
import com.team19.musuimsa.user.dto.UserLocationUpdateRequest;
import com.team19.musuimsa.user.dto.UserPasswordUpdateRequest;
import com.team19.musuimsa.user.dto.UserResponse;
import com.team19.musuimsa.user.dto.UserUpdateRequest;
import com.team19.musuimsa.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 API", description = "사용자 인증 및 정보 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원 가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "회원가입 성공 (헤더 Location에 사용자 리소스 URI 포함)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signUpUser(
            @Parameter(description = "회원가입 정보", required = true,
                    schema = @Schema(implementation = SignUpRequest.class))
            @Valid @RequestBody SignUpRequest signUpRequest
    ) {
        Long userId = userService.signUp(signUpRequest);

        URI location = URI.create("/api/users/" + userId);

        return ResponseEntity.created(location).body("회원가입이 성공적으로 완료되었습니다.");
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (유효성 검사 실패)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(
            @Parameter(description = "로그인 정보", required = true,
                    schema = @Schema(implementation = LoginRequest.class))
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        TokenResponse tokenResponse = userService.login(loginRequest);

        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "로그아웃", description = "서버에 저장된 사용자의 Refresh Token을 무효화합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 Access Token)")
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    ) {
        userService.logout(user);

        return ResponseEntity.ok("성공적으로 로그아웃 되었습니다.");
    }

    @Operation(summary = "토큰 재발급",
            description = "유효한 Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다.")
    @Parameters({
            @Parameter(name = "Authorization-Refresh", in = ParameterIn.HEADER,
                    description = "Refresh Token (Bearer 포함)", required = true,
                    example = "Bearer eyJhbGci...")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissueTokens(
            @RequestHeader("Authorization-Refresh") String refreshToken
    ) {
        TokenResponse tokenResponse = userService.reissueToken(refreshToken);

        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "내 정보 조회", description = "로그인된 사용자의 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    ) {
        UserResponse userInfo = userService.getUserInfo(user.getUserId());

        return ResponseEntity.ok(userInfo);
    }

    @Operation(summary = "특정 사용자 정보 조회", description = "사용자 ID를 이용하여 특정 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserInfo(
            @Parameter(description = "조회할 사용자의 ID", example = "1", required = true)
            @PathVariable Long userId
    ) {
        UserResponse userInfo = userService.getUserInfo(userId);

        return ResponseEntity.ok(userInfo);
    }

    @Operation(summary = "내 정보 수정", description = "로그인된 사용자의 닉네임 또는 프로필 이미지 URL을 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "409", description = "닉네임 중복",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateUserInfo(
            @Parameter(description = "수정할 사용자 정보 (닉네임, 프로필 이미지 URL 중 변경할 값만 포함)", required = true,
                    schema = @Schema(implementation = UserUpdateRequest.class))
            @RequestBody UserUpdateRequest userUpdateRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    ) {
        UserResponse updatedUser = userService.updateUserInfo(userUpdateRequest,
                user);

        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "비밀번호 변경", description = "로그인된 사용자의 비밀번호를 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400",
                    description = "잘못된 요청 데이터 (현재/새 비밀번호 누락 또는 유효성 검사 실패)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 또는 현재 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PatchMapping("/me/password")
    public ResponseEntity<String> updateUserPassword(
            @Parameter(description = "현재 비밀번호와 새 비밀번호", required = true,
                    schema = @Schema(implementation = UserPasswordUpdateRequest.class))
            @Valid @RequestBody UserPasswordUpdateRequest requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    ) {
        userService.updateUserPassword(requestDto, user);

        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    @Operation(summary = "회원 탈퇴", description = "로그인된 사용자의 계정을 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    ) {
        userService.deleteUser(user);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "사용자 위치 정보 업데이트",
            description = "로그인된 사용자의 마지막 위치 정보를 업데이트합니다. (푸시 알림 등에 활용)")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "위치 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 위치 데이터 형식"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping("/me/location")
    public ResponseEntity<Void> updateUserLocation(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "위도, 경도 정보", required = true,
                    schema = @Schema(implementation = UserLocationUpdateRequest.class))
            @RequestBody UserLocationUpdateRequest request) {
        userService.updateUserLocation(user.getUserId(), request);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자 FCM 기기 토큰 등록", description = "푸시 알림을 위한 사용자의 FCM 기기 토큰을 등록합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "기기 토큰 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 기기 토큰 형식"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
    })
    @PostMapping("/me/device")
    public ResponseEntity<Void> registerUserDevice(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "FCM 기기 토큰 정보", required = true,
                    schema = @Schema(implementation = UserDeviceRegisterRequest.class))
            @RequestBody UserDeviceRegisterRequest request) {
        UserDevice savedDevice = userService.registerUserDevice(user.getUserId(),
                request);

        URI location = URI.create("/api/users/me/devices/" + savedDevice.getId());

        return ResponseEntity.created(location).build();
    }
}
