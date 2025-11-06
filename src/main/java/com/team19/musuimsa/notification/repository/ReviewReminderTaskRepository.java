package com.team19.musuimsa.notification.repository;

import com.team19.musuimsa.notification.domain.ReviewReminderTask;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewReminderTaskRepository extends JpaRepository<ReviewReminderTask, Long> {

    // 발송 시간이 되었지만 아직 발송되지 않은 모든 알림 작업을 조회합니다. (N+1 방지 Fetch Join)
    @Query("SELECT r FROM ReviewReminderTask r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH r.shelter s " +
            "WHERE r.sent = false AND r.notifyAt <= :now")
    List<ReviewReminderTask> findPendingTasks(@Param("now") LocalDateTime now);

    // 발송 완료(sent = true)되었고, 특정 시간(cutoff) 이전에 예약되었던 오래된 작업을 삭제합니다.
    @Modifying
    @Query("DELETE FROM ReviewReminderTask r WHERE r.sent = true AND r.notifyAt < :cutoff")
    int deleteSentTasksBefore(@Param("cutoff") LocalDateTime cutoff);
}