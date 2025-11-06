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
import com.team19.musuimsa.user.service.UserPhotoService;
import com.team19.musuimsa.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@Tag(name = "사용자 API", description = "사용자 인증 및 정보 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final UserPhotoService userPhotoService;

    @Operation(summary = "회원 가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "회원가입 성공 (헤더 Location에 사용자 리소스 URI 포함)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "회원가입이 성공적으로 완료되었습니다."))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (유효성 검사 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "유효성 검사 실패",
                                    value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"email: 유효한 이메일 형식이 아닙니다., password: 비밀번호는 8자 이상 20자 이하로 입력해주세요.\", \"path\": \"/api/users/signup\"}"))),
            @ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = {
                                    @ExampleObject(name = "이메일 중복",
                                            value = "{\"status\": 409, \"error\": \"Conflict\", \"message\": \"이미 사용 중인 이메일입니다: user@example.com\", \"path\": \"/api/users/signup\"}"),
                                    @ExampleObject(name = "닉네임 중복",
                                            value = "{\"status\": 409, \"error\": \"Conflict\", \"message\": \"이미 사용 중인 닉네임입니다: testUser\", \"path\": \"/api/users/signup\"}")
                            }))
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signUpUser(
            @Parameter(description = "회원가입 정보", required = true,
                    schema = @Schema(implementation = SignUpRequest.class),
                    examples = @ExampleObject(name = "표준 회원가입 요청",
                            value = "{\"email\": \"user@example.com\", \"password\": \"password123!\", \"nickname\": \"무더위쉼터탐험가\", \"profileImageUrl\": \"https://example.com/profile.jpg\"}")
            )
            @Valid @RequestBody SignUpRequest signUpRequest
    ) {
        Long userId = userService.signUp(signUpRequest);

        URI location = URI.create("/api/users/" + userId);

        return ResponseEntity.created(location).body("회원가입이 성공적으로 완료되었습니다.");
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class),
                            examples = @ExampleObject(name = "로그인 성공",
                                    value = "{\"accessToken\": \"eyJhb...\", \"refreshToken\": \"eyJhb...\"}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (유효성 검사 실패)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "유효성 검사 실패",
                                    value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"email: 이메일은 필수 입력 값입니다.\", \"path\": \"/api/users/login\"}"))),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "로그인 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"이메일 또는 비밀번호가 일치하지 않습니다. \", \"path\": \"/api/users/login\"}")))
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(
            @Parameter(description = "로그인 정보", required = true,
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(name = "로그인 요청",
                            value = "{\"email\": \"user@example.com\", \"password\": \"password123!\"}")
            )
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        TokenResponse tokenResponse = userService.login(loginRequest);

        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "로그아웃", description = "서버에 저장된 사용자의 Refresh Token을 무효화합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "성공적으로 로그아웃 되었습니다."))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 Access Token)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "인증 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"회원가입 또는 로그인 후 이용 가능합니다. \", \"path\": \"/api/users/logout\"}")))
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
                    content = @Content(schema = @Schema(implementation = TokenResponse.class),
                            examples = @ExampleObject(name = "토큰 재발급 성공",
                                    value = "{\"accessToken\": \"eyJhb...\", \"refreshToken\": \"eyJhb...\"}"))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "토큰 무효",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"유효하지 않은 리프레시 토큰입니다. 다시 로그인 해주세요.\", \"path\": \"/api/users/reissue\"}")))
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
                    content = @Content(schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(name = "내 정보 조회",
                                    value = "{\"userId\": 1, \"email\": \"user@example.com\", \"nickname\": \"무더위쉼터탐험가\", \"profileImageUrl\": \"https://example.com/profile.jpg\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "인증 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"회원가입 또는 로그인 후 이용 가능합니다. \", \"path\": \"/api/users/me\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "사용자 없음",
                                    value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"해당 ID의 사용자를 찾을 수 없습니다: 1\", \"path\": \"/api/users/me\"}")))
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    ) {
        UserResponse userInfo = userService.getUserInfo(user.getUserId());

        return ResponseEntity.ok(userPhotoService.signIfPresent(userInfo));
    }

    @Operation(summary = "내 정보 수정", description = "로그인된 사용자의 닉네임 또는 프로필 이미지 URL을 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(name = "정보 수정 성공",
                                    value = "{\"userId\": 1, \"email\": \"user@example.com\", \"nickname\": \"새로운닉네임\", \"profileImageUrl\": \"https://example.com/new.jpg\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "인증 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"회원가입 또는 로그인 후 이용 가능합니다. \", \"path\": \"/api/users/me\"}"))),
            @ApiResponse(responseCode = "409", description = "닉네임 중복",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "닉네임 중복",
                                    value = "{\"status\": 409, \"error\": \"Conflict\", \"message\": \"이미 사용 중인 닉네임입니다: newNickname\", \"path\": \"/api/users/me\"}")))
    })
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateUserInfo(
            @Parameter(description = "수정할 사용자 정보 (닉네임, 프로필 이미지 URL 중 변경할 값만 포함)", required = true,
                    schema = @Schema(implementation = UserUpdateRequest.class),
                    examples = {
                            @ExampleObject(name = "닉네임만 변경",
                                    value = "{\"nickname\": \"새로운닉네임\"}"),
                            @ExampleObject(name = "사진만 변경",
                                    value = "{\"profileImageUrl\": \"https://example.com/new.jpg\"}"),
                            @ExampleObject(name = "둘 다 변경",
                                    value = "{\"nickname\": \"새로운닉네임\", \"profileImageUrl\": \"https://example.com/new.jpg\"}")
                    }
            )
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
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "비밀번호가 성공적으로 변경되었습니다."))),
            @ApiResponse(responseCode = "400",
                    description = "잘못된 요청 데이터 (현재/새 비밀번호 유효성 검사 실패)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "유효성 검사 실패",
                                    value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"currentPassword: 현재 비밀번호는 필수 입력값입니다.\", \"path\": \"/api/users/me/password\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 실패 또는 현재 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "비밀번호 불일치",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"현재 비밀번호가 일치하지 않습니다.\", \"path\": \"/api/users/me/password\"}")))
    })
    @PatchMapping("/me/password")
    public ResponseEntity<String> updateUserPassword(
            @Parameter(description = "현재 비밀번호와 새 비밀번호", required = true,
                    schema = @Schema(implementation = UserPasswordUpdateRequest.class),
                    examples = @ExampleObject(name = "비밀번호 변경 요청",
                            value = "{\"currentPassword\": \"password123!\", \"newPassword\": \"newPassword456@\"}")
            )
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
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "인증 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"회원가입 또는 로그인 후 이용 가능합니다. \", \"path\": \"/api/users/me\"}")))
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
            @ApiResponse(responseCode = "400", description = "잘못된 위치 데이터 형식",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "유효성 검사 실패",
                                    value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"latitude: 값이 없거나 유효하지 않습니다.\", \"path\": \"/api/users/me/location\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "인증 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"회원가입 또는 로그인 후 이용 가능합니다. \", \"path\": \"/api/users/me/location\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "사용자 없음",
                                    value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"해당 ID의 사용자를 찾을 수 없습니다: 1\", \"path\": \"/api/users/me/location\"}")))
    })
    @PostMapping("/me/location")
    public ResponseEntity<Void> updateUserLocation(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "위도, 경도 정보", required = true,
                    schema = @Schema(implementation = UserLocationUpdateRequest.class),
                    examples = @ExampleObject(name = "위치 정보",
                            value = "{\"latitude\": 37.5665, \"longitude\": 126.9780}")
            )
            @RequestBody UserLocationUpdateRequest request) {
        userService.updateUserLocation(user.getUserId(), request);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자 FCM 기기 토큰 등록", description = "푸시 알림을 위한 사용자의 FCM 기기 토큰을 등록합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "기기 토큰 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 기기 토큰 형식",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "유효성 검사 실패",
                                    value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"deviceToken: 값이 없거나 유효하지 않습니다.\", \"path\": \"/api/users/me/device\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "인증 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"회원가입 또는 로그인 후 이용 가능합니다. \", \"path\": \"/api/users/me/device\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "사용자 없음",
                                    value = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"해당 ID의 사용자를 찾을 수 없습니다: 1\", \"path\": \"/api/users/me/device\"}"))),
    })
    @PostMapping("/me/device")
    public ResponseEntity<Void> registerUserDevice(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "FCM 기기 토큰 정보", required = true,
                    schema = @Schema(implementation = UserDeviceRegisterRequest.class),
                    examples = @ExampleObject(name = "FCM 토큰",
                            value = "{\"deviceToken\": \"fcm_token_string_from_client_app...\"}")
            )
            @RequestBody UserDeviceRegisterRequest request) {
        UserDevice savedDevice = userService.registerUserDevice(user.getUserId(),
                request);

        URI location = URI.create("/api/users/me/devices/" + savedDevice.getId());

        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "내 프로필 이미지 업로드",
            description = "멀티파트 이미지 업로드 후 사용자 프로필 이미지 URL을 갱신합니다. 응답으로 받는 URL은 15분간 유효한 Presigned URL입니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드/갱신 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(name = "프로필 이미지 업로드 성공",
                                    value = "{\"userId\": 1, \"email\": \"user@example.com\", \"nickname\": \"무더위쉼터탐험가\", \"profileImageUrl\": \"https://musuimsa.s3.ap-northeast-2.amazonaws.com/users/1/uuid.jpg?AWSAccessKeyId=...&Expires=...&Signature=...\"}"))),
            @ApiResponse(responseCode = "400", description = "파일 누락/형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = {
                                    @ExampleObject(name = "파일 형식 오류",
                                            value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"지원하지 않는 이미지 형식입니다: image/gif (허용: image/jpeg, image/png, image/webp)\", \"path\": \"/api/users/me/profile-image\"}"),
                                    @ExampleObject(name = "파일 누락",
                                            value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"업로드할 파일이 비어있습니다.\", \"path\": \"/api/users/me/profile-image\"}"),
                                    @ExampleObject(name = "파일 크기 초과",
                                            value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"파일 크기가 너무 큽니다. 최대 10MB까지 업로드 가능합니다.\", \"path\": \"/api/users/me/profile-image\"}")
                            })),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "인증 실패",
                                    value = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"회원가입 또는 로그인 후 이용 가능합니다. \", \"path\": \"/api/users/me/profile-image\"}")))
    })
    @PostMapping(value = "/me/profile-image", consumes = "multipart/form-data")
    public ResponseEntity<UserResponse> uploadMyProfileImage(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user,
            @RequestPart("file") MultipartFile file
    ) {
        UserResponse updated = userPhotoService.changeMyProfileImage(user, file);
        return ResponseEntity.ok(updated);
    }
}
