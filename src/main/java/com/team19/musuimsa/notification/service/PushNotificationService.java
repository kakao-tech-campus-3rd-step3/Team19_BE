package com.team19.musuimsa.notification.service;

import com.team19.musuimsa.exception.external.ExternalApiException;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.repository.UserRepository;
import com.team19.musuimsa.weather.dto.WeatherResponse;
import com.team19.musuimsa.weather.service.WeatherService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PushNotificationService {

    private static final double TEMPERATURE_THRESHOLD = 10.0;
    private static final int NOTIFICATION_COOLDOWN_MINUTES = 1;

    private final UserRepository userRepository;
    private final WeatherService weatherService;
    private final FcmService fcmService;

    public void checkUsersAndSendPushNotifications() {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            if (!user.hasLocation()) {
                continue;
            }
            checkTemperatureAndSendPush(user);
        }
    }

    private void checkTemperatureAndSendPush(User user) {
        try {
            double lat = user.getLastLatitude().doubleValue();
            double lon = user.getLastLongitude().doubleValue();
            WeatherResponse weather = weatherService.getCurrentTemp(lat, lon);
            double currentTemp = weather.temperature();

            log.debug("User: {}, Temp: {}°C", user.getNickname(), currentTemp);

            if (isAbleToSendHeatwaveAlert(user, currentTemp)) {
                log.info("Sending heatwave alert to user: {}", user.getNickname());
                String title = "날씨가 많이 덥습니다!";
                String body = String.format("현재 계신 곳의 온도가 %.1f°C 입니다. 근처 무더위 쉼터를 찾으려면 누르세요!",
                        currentTemp);

                boolean sentSuccessfully = fcmService.sendPushNotification(user.getUserId(), title,
                        body);

                if (sentSuccessfully) {
                    user.updateLastHeatwaveAlertAt();
                }
            }
        } catch (ExternalApiException e) {
            log.warn("날씨 API 호출에 실패하여 푸시 알림을 처리하지 못했습니다. User ID: {}, URL: {}", user.getUserId(),
                    e.getUrl());
        } catch (Exception e) {
            log.error("푸시 알림 처리 중 예상치 못한 오류가 발생했습니다. User ID: {}", user.getUserId(), e);
        }
    }

    private boolean isAbleToSendHeatwaveAlert(User user, double currentTemp) {
        boolean isHot = currentTemp >= TEMPERATURE_THRESHOLD;

        if (!isHot) {
            return false;
        }

        return user.getLastHeatwaveAlertAt() == null ||
                user.getLastHeatwaveAlertAt()
                        .isBefore(LocalDateTime.now()
                                .minusMinutes(NOTIFICATION_COOLDOWN_MINUTES));
    }
}