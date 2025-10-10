package com.team19.musuimsa.user.service;

import com.team19.musuimsa.exception.auth.InvalidPasswordException;
import com.team19.musuimsa.exception.auth.InvalidRefreshTokenException;
import com.team19.musuimsa.exception.auth.LoginFailedException;
import com.team19.musuimsa.exception.conflict.EmailDuplicateException;
import com.team19.musuimsa.exception.conflict.NicknameDuplicateException;
import com.team19.musuimsa.exception.notfound.UserNotFoundException;
import com.team19.musuimsa.user.domain.RefreshToken;
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
import com.team19.musuimsa.user.repository.UserDeviceRepository;
import com.team19.musuimsa.user.repository.UserRepository;
import com.team19.musuimsa.util.JwtUtil;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://wikis.krsocsci.org/images/a/aa/%EA%B8%B0%EB%B3%B8_%ED%94%84%EB%A1%9C%ED%95%84.png";

    public Long signUp(SignUpRequest signUpRequest) {
        checkDuplicateUser(signUpRequest);

        String encodedPassword = passwordEncoder.encode(signUpRequest.password());

        String profileImageUrl = signUpRequest.profileImageUrl();
        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
            profileImageUrl = DEFAULT_PROFILE_IMAGE_URL;
        }

        User user = new User(
                signUpRequest.email(),
                encodedPassword,
                signUpRequest.nickname(),
                profileImageUrl
        );

        User savedUser = userRepository.save(user);

        return savedUser.getUserId();
    }

    public TokenResponse login(LoginRequest loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.email())
                .orElseThrow(LoginFailedException::new);

        if (!passwordEncoder.matches(loginRequestDto.password(), user.getPassword())) {
            throw new LoginFailedException();
        }

        String accessToken = jwtUtil.createAccessToken(user.getEmail());
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());

        user.updateRefreshToken(refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    public void logout(User loginUser) {
        User user = getUserById(loginUser.getUserId());
        user.invalidateRefreshToken();
    }

    public TokenResponse reissueToken(String refreshToken) {
        // "Bearer " 접두사 제거
        RefreshToken token = RefreshToken.from(refreshToken);
        String pureToken = token.getPureToken();

        // 1. 토큰 검증
        if (!jwtUtil.validateToken(pureToken)) {
            throw new InvalidRefreshTokenException();
        }

        // 2. 토큰에서 사용자 정보 추출
        String email = jwtUtil.getUserInfoFromToken(pureToken).getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidRefreshTokenException::new);

        // 3. DB에 저장된 토큰과 일치하는지 확인
        if (user.getRefreshToken() == null || !user.getRefreshToken().getToken()
                .equals(pureToken)) {
            throw new InvalidRefreshTokenException();
        }

        // 4. 새로운 Access Token 생성
        String newAccessToken = jwtUtil.createAccessToken(email);

        // 5. Refresh Token 새로 발급
        String newRefreshToken = jwtUtil.createRefreshToken(email);
        user.updateRefreshToken(newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserInfo(Long userId) {
        User user = getUserById(userId);

        return UserResponse.from(user);
    }

    public UserResponse updateUserInfo(UserUpdateRequest userUpdateRequest, User loginUser) {
        String newNickname = userUpdateRequest.nickname();
        String newProfileImageUrl = userUpdateRequest.profileImageUrl();

        boolean isNicknameSame = Objects.equals(newNickname, loginUser.getNickname());
        boolean isProfileImageUrlSame = Objects.equals(newProfileImageUrl,
                loginUser.getProfileImageUrl());

        if (isNicknameSame && isProfileImageUrlSame) {
            return UserResponse.from(loginUser);
        }

        if (!isNicknameSame && newNickname != null && !newNickname.isEmpty()) {
            userRepository.findByNickname(newNickname).ifPresent(existingUer -> {
                throw new NicknameDuplicateException(newNickname);
            });
        }

        loginUser.updateUser(newNickname, newProfileImageUrl);

        return UserResponse.from(loginUser);
    }

    public void updateUserPassword(UserPasswordUpdateRequest requestDto, User loginUser) {
        if (!passwordEncoder.matches(requestDto.currentPassword(), loginUser.getPassword())) {
            throw new InvalidPasswordException();
        }

        String newEncodedPassword = passwordEncoder.encode(requestDto.newPassword());
        loginUser.updatePassword(newEncodedPassword);
    }

    public void deleteUser(User loginUser) {
        userRepository.delete(loginUser);
    }

    private void checkDuplicateUser(SignUpRequest signUpRequest) {
        userRepository.findByEmail(signUpRequest.email()).ifPresent(user -> {
            throw new EmailDuplicateException(signUpRequest.email());
        });

        userRepository.findByNickname(signUpRequest.nickname()).ifPresent(user -> {
            throw new NicknameDuplicateException(signUpRequest.nickname());
        });
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public void updateUserLocation(Long userId, UserLocationUpdateRequest request) {
        User user = getUserById(userId);
        user.updateLocation(request.latitude(), request.longitude());
    }

    public UserDevice registerUserDevice(Long userId, UserDeviceRegisterRequest request) {
        User user = getUserById(userId);
        UserDevice userDevice = new UserDevice(user, request.deviceToken());

        return userDeviceRepository.save(userDevice);
    }
}
