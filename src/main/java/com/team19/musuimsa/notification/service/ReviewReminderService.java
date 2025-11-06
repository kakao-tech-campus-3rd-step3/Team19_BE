package com.team19.musuimsa.notification.service;

import com.team19.musuimsa.exception.notfound.ShelterNotFoundException;
import com.team19.musuimsa.notification.domain.ReviewReminderTask;
import com.team19.musuimsa.notification.repository.ReviewReminderTaskRepository;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.repository.UserRepository;
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
public class ReviewReminderService {

    private final ReviewReminderTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ShelterRepository shelterRepository;
    private final FcmService fcmService;

    // 알림 지연 시간 (10분)
    private static final int DELAY_MINUTES = 10;

    // 클라이언트로부터 쉼터 도착 알림을 받아 10분 뒤 리뷰 알림을 예약합니다.
    public void scheduleReviewReminder(Long shelterId, User loginUser) {
        // User와 Shelter는 프록시(참조)만 가져옵니다.
        User user = userRepository.getReferenceById(loginUser.getUserId());
        Shelter shelter = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        LocalDateTime notifyAt = LocalDateTime.now().plusMinutes(DELAY_MINUTES);

        ReviewReminderTask task = new ReviewReminderTask(user, shelter, notifyAt);
        taskRepository.save(task);

        log.info("Review reminder scheduled for user {} at shelter {} (NotifyAt: {})",
                user.getUserId(), shelter.getShelterId(), notifyAt);
    }

    // 예약된 리뷰 알림을 발송합니다. (스케줄러에 의해 1분마다 호출됨)
    public void processPendingReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<ReviewReminderTask> tasks = taskRepository.findPendingTasks(now);

        if (tasks.isEmpty()) {
            return; // 처리할 작업 없음
        }

        log.info("... Found {} review reminders to send.", tasks.size());
        int successCount = 0;
        int failCount = 0;

        for (ReviewReminderTask task : tasks) {
            try {
                String title = task.getShelter().getName() + " 이용은 어떠셨나요?";
                String body = String.format("%d분 전 방문하신 쉼터의 소중한 리뷰를 남겨주세요!", DELAY_MINUTES);

                boolean sent = fcmService.sendPushNotification(
                        task.getUser().getUserId(), title, body
                );

                if (sent) {
                    successCount++;
                } else {
                    log.warn("Failed to send review reminder (FCM returned false) for task ID: {}",
                            task.getId());
                    failCount++;
                }
            } catch (Exception e) {
                log.error("Error processing review reminder task ID: {}", task.getId(), e);
                failCount++;
            } finally {
                // 성공 여부와 관계없이 중복 발송을 막기 위해 '발송됨'으로 처리
                task.markAsSent();
                taskRepository.save(task);
            }
        }
        log.info("... Review reminder processing finished. (Success: {}, Fail: {})", successCount,
                failCount);
    }
}