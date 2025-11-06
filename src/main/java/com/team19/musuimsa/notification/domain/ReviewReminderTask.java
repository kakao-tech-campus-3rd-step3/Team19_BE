package com.team19.musuimsa.notification.domain;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_reminders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReminderTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelter_id", nullable = false)
    private Shelter shelter;

    @Column(nullable = false)
    private LocalDateTime notifyAt; // 알림 발송 예정 시각

    @Column(nullable = false)
    private boolean sent = false; // 발송 완료 여부

    public ReviewReminderTask(User user, Shelter shelter, LocalDateTime notifyAt) {
        this.user = user;
        this.shelter = shelter;
        this.notifyAt = notifyAt;
    }

    public void markAsSent() {
        this.sent = true;
    }
}