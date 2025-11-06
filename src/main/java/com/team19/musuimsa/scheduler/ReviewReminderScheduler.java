package com.team19.musuimsa.scheduler;

import com.team19.musuimsa.notification.service.ReviewReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewReminderScheduler {

    private final ReviewReminderService reviewReminderService;

    // 매 분 0초마다 실행되어, 알림 시간이 도래한 리뷰 요청을 발송합니다.
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void runReviewReminderJob() {
        log.info(">> Starting review reminder job (every minute)...");
        try {
            reviewReminderService.processPendingReminders();
        } catch (Exception e) {
            // 스케줄러 스레드에서 발생하는 예외가 전파되어 작업이 중단되는 것을 방지
            log.error("!! Review reminder job failed with an unhandled exception.", e);
        }
        log.info("<< Finished review reminder job.");
    }
}