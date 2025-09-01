package com.team19.musuimsa.user.controller;

import com.team19.musuimsa.security.UserDetailsImpl;
import com.team19.musuimsa.user.dto.LoginRequestDto;
import com.team19.musuimsa.user.dto.SignUpRequestDto;
import com.team19.musuimsa.user.dto.TokenResponseDto;
import com.team19.musuimsa.user.dto.UserPasswordUpdateRequestDto;
import com.team19.musuimsa.user.dto.UserResponseDto;
import com.team19.musuimsa.user.dto.UserUpdateRequestDto;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signUpUser(
            @Valid @RequestBody SignUpRequestDto signUpRequestDto
    ) {
        Long userId = userService.signUp(signUpRequestDto);

        URI location = URI.create("/api/users/" + userId);

        return ResponseEntity.created(location).body("회원가입이 성공적으로 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> loginUser(
            @Valid @RequestBody LoginRequestDto loginRequestDto
    ) {
        TokenResponseDto tokenResponseDto = userService.login(loginRequestDto);

        return ResponseEntity.ok(tokenResponseDto);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUserInfo(
            @PathVariable Long userId
    ) {
        UserResponseDto userInfo = userService.getUserInfo(userId);

        return ResponseEntity.ok(userInfo);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponseDto> updateUserInfo(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequestDto userUpdateRequestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        UserResponseDto updatedUser = userService.updateUserInfo(userId, userUpdateRequestDto,
                userDetails.getUser());

        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<String> updateUserPassword(
            @PathVariable Long userId,
            @Valid @RequestBody UserPasswordUpdateRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        userService.updateUserPassword(userId, requestDto, userDetails.getUser());

        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        userService.deleteUser(userId, userDetails.getUser());

        return ResponseEntity.noContent().build();
    }
}
