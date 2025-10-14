package com.team19.musuimsa.notification.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.repository.UserRepository;
import com.team19.musuimsa.weather.dto.WeatherResponse;
import com.team19.musuimsa.weather.service.WeatherService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @InjectMocks
    private PushNotificationService pushNotificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WeatherService weatherService;

    @Mock
    private FcmService fcmService;

    private User userWithLocation;
    private User userWithoutLocation;

    @BeforeEach
    void setUp() {
        userWithLocation = new User("test@example.com", "password", "testUser", "profile.jpg");
        ReflectionTestUtils.setField(userWithLocation, "userId", 1L);
        userWithLocation.updateLocation(new BigDecimal("36.3504"), new BigDecimal("127.3845"));

        userWithoutLocation = new User("nouser@example.com", "password", "noLocationUser",
                "profile.jpg");
        ReflectionTestUtils.setField(userWithoutLocation, "userId", 2L);
    }

    @Test
    @DisplayName("기온이 35도 이상이고 쿨다운이 지났을 때 푸시 알림을 보낸다")
    void sendPush_When_TemperatureIsHigh_And_CooldownPassed() {
        // given
        WeatherResponse weatherResponse = new WeatherResponse(35.0, "20251009", "1400");
        when(userRepository.findAll()).thenReturn(List.of(userWithLocation));
        when(weatherService.getCurrentTemp(anyDouble(), anyDouble())).thenReturn(weatherResponse);
        when(fcmService.sendPushNotification(anyLong(), anyString(), anyString())).thenReturn(true);

        // when
        pushNotificationService.checkUsersAndSendPushNotifications();

        // then
        verify(fcmService, times(1)).sendPushNotification(eq(userWithLocation.getUserId()),
                anyString(), anyString());
        assertThat(userWithLocation.getLastHeatwaveAlertAt()).isNotNull();
    }

    @Test
    @DisplayName("기온이 35도 미만일 때 푸시 알림을 보내지 않는다")
    void doNotSendPush_When_TemperatureIsLow() {
        // given
        WeatherResponse weatherResponse = new WeatherResponse(34.9, "20251009", "1400");
        when(userRepository.findAll()).thenReturn(List.of(userWithLocation));
        when(weatherService.getCurrentTemp(anyDouble(), anyDouble())).thenReturn(weatherResponse);

        // when
        pushNotificationService.checkUsersAndSendPushNotifications();

        // then
        verify(fcmService, never()).sendPushNotification(anyLong(), anyString(), anyString());
        assertThat(userWithLocation.getLastHeatwaveAlertAt()).isNull();
    }

    @Test
    @DisplayName("기온이 35도 이상이지만 쿨다운이 지나지 않았을 때 푸시 알림을 보내지 않는다")
    void doNotSendPush_When_TemperatureIsHigh_And_CooldownNotPassed() {
        // given
        userWithLocation.updateLastHeatwaveAlertAt(); // 현재 시간으로 마지막 알림 시간 설정
        WeatherResponse weatherResponse = new WeatherResponse(36.0, "20251009", "1400");
        when(userRepository.findAll()).thenReturn(List.of(userWithLocation));
        when(weatherService.getCurrentTemp(anyDouble(), anyDouble())).thenReturn(weatherResponse);

        // when
        pushNotificationService.checkUsersAndSendPushNotifications();

        // then
        verify(fcmService, never()).sendPushNotification(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("쿨다운 경계값 테스트: 50분 직전에는 알림을 보내지 않는다")
    void doNotSendPush_When_JustBeforeCooldownExpires() {
        // given
        LocalDateTime alertTime = LocalDateTime.now().minusMinutes(50).plusSeconds(1);
        ReflectionTestUtils.setField(userWithLocation, "lastHeatwaveAlertAt", alertTime);

        WeatherResponse weatherResponse = new WeatherResponse(35.0, "20251010", "1500");
        when(userRepository.findAll()).thenReturn(List.of(userWithLocation));
        when(weatherService.getCurrentTemp(anyDouble(), anyDouble())).thenReturn(weatherResponse);

        // when
        pushNotificationService.checkUsersAndSendPushNotifications();

        // then
        verify(fcmService, never()).sendPushNotification(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("쿨다운 경계값 테스트: 정확히 50분이 지났을 때 알림을 보낸다")
    void sendPush_When_CooldownIsExactlyMet() {
        // given
        LocalDateTime alertTime = LocalDateTime.now().minusMinutes(50);
        ReflectionTestUtils.setField(userWithLocation, "lastHeatwaveAlertAt", alertTime);

        WeatherResponse weatherResponse = new WeatherResponse(35.0, "20251010", "1500");
        when(userRepository.findAll()).thenReturn(List.of(userWithLocation));
        when(weatherService.getCurrentTemp(anyDouble(), anyDouble())).thenReturn(weatherResponse);

        // when
        pushNotificationService.checkUsersAndSendPushNotifications();

        // then
        verify(fcmService, times(1)).sendPushNotification(eq(userWithLocation.getUserId()),
                anyString(), anyString());
    }

    @Test
    @DisplayName("사용자의 위치 정보가 없을 경우 확인 대상에서 제외된다")
    void skipUser_When_LocationIsNotSet() {
        // given
        when(userRepository.findAll()).thenReturn(List.of(userWithoutLocation));

        // when
        pushNotificationService.checkUsersAndSendPushNotifications();

        // then
        verify(weatherService, never()).getCurrentTemp(anyDouble(), anyDouble());
        verify(fcmService, never()).sendPushNotification(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("날씨 정보 조회에 실패하면 푸시 알림을 보내지 않는다")
    void doNotSendPush_When_WeatherServiceFails() {
        // given
        when(userRepository.findAll()).thenReturn(List.of(userWithLocation));
        when(weatherService.getCurrentTemp(anyDouble(), anyDouble())).thenThrow(
                new RuntimeException("API Error"));

        // when
        pushNotificationService.checkUsersAndSendPushNotifications();

        // then
        verify(fcmService, never()).sendPushNotification(anyLong(), anyString(), anyString());
    }
}