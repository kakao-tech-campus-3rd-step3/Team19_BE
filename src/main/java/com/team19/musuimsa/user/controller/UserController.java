package com.team19.musuimsa.user.controller;

import com.team19.musuimsa.security.UserDetailsImpl;
import com.team19.musuimsa.user.dto.LoginRequest;
import com.team19.musuimsa.user.dto.SignUpRequest;
import com.team19.musuimsa.user.dto.TokenResponse;
import com.team19.musuimsa.user.dto.UserDeviceRegisterRequest;
import com.team19.musuimsa.user.dto.UserLocationUpdateRequest;
import com.team19.musuimsa.user.dto.UserPasswordUpdateRequest;
import com.team19.musuimsa.user.dto.UserResponse;
import com.team19.musuimsa.user.dto.UserUpdateRequest;
import com.team19.musuimsa.user.service.UserService;
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signUpUser(
            @Valid @RequestBody SignUpRequest signUpRequest
    ) {
        Long userId = userService.signUp(signUpRequest);

        URI location = URI.create("/api/users/" + userId);

        return ResponseEntity.created(location).body("회원가입이 성공적으로 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        TokenResponse tokenResponse = userService.login(loginRequest);

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        userService.logout(userDetails.getUser());

        return ResponseEntity.ok("성공적으로 로그아웃 되었습니다.");
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissueTokens(
            @RequestHeader("Authorization-Refresh") String refreshToken
    ) {
        TokenResponse tokenResponse = userService.reissueToken(refreshToken);

        return ResponseEntity.ok(tokenResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        UserResponse userInfo = userService.getUserInfo(userDetails.getUser().getUserId());

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserInfo(
            @PathVariable Long userId
    ) {
        UserResponse userInfo = userService.getUserInfo(userId);

        return ResponseEntity.ok(userInfo);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateUserInfo(
            @RequestBody UserUpdateRequest userUpdateRequest,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        UserResponse updatedUser = userService.updateUserInfo(userUpdateRequest,
                userDetails.getUser());

        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<String> updateUserPassword(
            @Valid @RequestBody UserPasswordUpdateRequest requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        userService.updateUserPassword(requestDto, userDetails.getUser());

        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        userService.deleteUser(userDetails.getUser());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/location")
    public ResponseEntity<Void> updateUserLocation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UserLocationUpdateRequest request) {
        userService.updateUserLocation(userDetails.getUser().getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/device")
    public ResponseEntity<Void> registerUserDevice(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UserDeviceRegisterRequest request) {
        userService.registerUserDevice(userDetails.getUser().getUserId(), request);
        return ResponseEntity.ok().build();
    }
}
