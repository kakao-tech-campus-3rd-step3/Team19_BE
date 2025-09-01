package com.team19.musuimsa.user.service;

import com.team19.musuimsa.exception.auth.AuthenticationException;
import com.team19.musuimsa.exception.auth.InvalidPasswordException;
import com.team19.musuimsa.exception.auth.InvalidRefreshTokenException;
import com.team19.musuimsa.exception.auth.LoginFailedException;
import com.team19.musuimsa.exception.conflict.EmailDuplicateException;
import com.team19.musuimsa.exception.conflict.NicknameDuplicateException;
import com.team19.musuimsa.exception.notfound.UserNotFoundException;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.dto.LoginRequestDto;
import com.team19.musuimsa.user.dto.SignUpRequestDto;
import com.team19.musuimsa.user.dto.TokenResponseDto;
import com.team19.musuimsa.user.dto.UserPasswordUpdateRequestDto;
import com.team19.musuimsa.user.dto.UserResponseDto;
import com.team19.musuimsa.user.dto.UserUpdateRequestDto;
import com.team19.musuimsa.user.repository.UserRepository;
import com.team19.musuimsa.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Long signUp(SignUpRequestDto signUpRequestDto) {
        checkDuplicateUser(signUpRequestDto);

        String encodedPassword = passwordEncoder.encode(signUpRequestDto.password());

        String profileImageUrl = signUpRequestDto.profileImageUrl();
        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
            profileImageUrl = "https://wikis.krsocsci.org/images/a/aa/%EA%B8%B0%EB%B3%B8_%ED%94%84%EB%A1%9C%ED%95%84.png";
        }

        User user = new User(
                signUpRequestDto.email(),
                encodedPassword,
                signUpRequestDto.nickname(),
                profileImageUrl
        );

        User savedUser = userRepository.save(user);

        return savedUser.getUserId();
    }

    public TokenResponseDto login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.email())
                .orElseThrow(LoginFailedException::new);

        if (!passwordEncoder.matches(loginRequestDto.password(), user.getPassword())) {
            throw new LoginFailedException();
        }

        String accessToken = jwtUtil.createAccessToken(user.getEmail());
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());

        user.updateRefreshToken(refreshToken);

        return new TokenResponseDto(accessToken, refreshToken);
    }

    public void logout(User loginUser) {
        User user = getUserById(loginUser.getUserId());
        user.invalidateRefreshToken();
    }

    public TokenResponseDto reissueToken(String refreshToken) {
        // "Bearer " 접두사 제거
        String pureToken = refreshToken.substring(7);

        // 1. 토큰 검증
        if (!jwtUtil.validateToken(pureToken)) {
            throw new InvalidRefreshTokenException();
        }

        // 2. 토큰에서 사용자 정보 추출
        String email = jwtUtil.getUserInfoFromToken(pureToken).getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidRefreshTokenException::new);

        // 3. DB에 저장된 토큰과 일치하는지 확인
        if (!user.getRefreshToken().equals(pureToken)) {
            throw new InvalidRefreshTokenException();
        }

        // 4. 새로운 Access Token 생성
        String newAccessToken = jwtUtil.createAccessToken(email);

        // 5. Refresh Token 새로 발급
        String newRefreshToken = jwtUtil.createRefreshToken(email);
        user.updateRefreshToken(newRefreshToken);

        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserInfo(Long userId) {
        User user = getUserById(userId);

        return UserResponseDto.from(user);
    }

    public UserResponseDto updateUserInfo(Long userId, UserUpdateRequestDto userUpdateRequestDto,
            User loginUser) {
        User user = getUserById(userId);

        validateUserPermission(user, loginUser, "자신의 정보만 수정할 수 있습니다.");

        String newNickname = userUpdateRequestDto.nickname();
        if (newNickname != null && !newNickname.equals(user.getNickname())) {
            userRepository.findByNickname(newNickname).ifPresent(existingUser -> {
                throw new NicknameDuplicateException(newNickname);
            });
        }

        user.updateUser(newNickname, userUpdateRequestDto.profileImageUrl());

        return UserResponseDto.from(user);
    }

    public void updateUserPassword(Long userId, UserPasswordUpdateRequestDto requestDto,
            User loginUser) {
        User user = getUserById(userId);

        validateUserPermission(user, loginUser, "자신의 비밀번호만 변경할 수 있습니다.");

        if (!passwordEncoder.matches(requestDto.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        String newEncodedPassword = passwordEncoder.encode(requestDto.newPassword());
        user.updatePassword(newEncodedPassword);
    }

    public void deleteUser(Long userId, User loginUser) {
        User user = getUserById(userId);

        validateUserPermission(user, loginUser, "자신만 탈퇴할 수 있습니다.");

        userRepository.delete(user);
    }

    private void checkDuplicateUser(SignUpRequestDto signUpRequestDto) {
        userRepository.findByEmail(signUpRequestDto.email()).ifPresent(user -> {
            throw new EmailDuplicateException(signUpRequestDto.email());
        });

        userRepository.findByNickname(signUpRequestDto.nickname()).ifPresent(user -> {
            throw new NicknameDuplicateException(signUpRequestDto.nickname());
        });
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void validateUserPermission(User targetUser, User loginUser, String errorMessage) {
        if (!targetUser.getUserId().equals(loginUser.getUserId())) {
            throw new AuthenticationException(errorMessage);
        }
    }
}
