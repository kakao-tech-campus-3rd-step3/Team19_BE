package com.team19.musuimsa.scheduler;

import com.team19.musuimsa.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationScheduler {

    private final PushNotificationService pushNotificationService;

    // 오전 8시부터 오후 10시까지 매 시간 정각에 실행 (서울 시간 기준)
    @Scheduled(cron = "0 0 8-22 * * *", zone = "Asia/Seoul")
    public void runHeatwaveCheck() {
        log.info(">> Starting hourly heatwave check job (8-22h)...");
        pushNotificationService.checkUsersAndSendPushNotifications();
        log.info("<< Finished hourly heatwave check job.");
    }
}