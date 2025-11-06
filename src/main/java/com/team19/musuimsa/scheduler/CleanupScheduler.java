package com.team19.musuimsa.scheduler;

import com.team19.musuimsa.notification.repository.ReviewReminderTaskRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final ReviewReminderTaskRepository taskRepository;

    // 알림 기록 보관 기간 (7일)
    private static final int RETAIN_DAYS = 7;

    // 매일 새벽 4시에 실행되어, 7일이 지난 '발송 완료(sent=true)' 알림 기록을 삭제합니다.
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOldReviewReminders() {
        log.info(">> Starting cleanup job for old review reminders...");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETAIN_DAYS);

        try {
            int deletedCount = taskRepository.deleteSentTasksBefore(cutoff);
            log.info("<< Finished cleanup job. Deleted {} tasks older than {}.", deletedCount,
                    cutoff);
        } catch (Exception e) {
            log.error("!! Cleanup job for review reminders failed.", e);
        }
    }
}