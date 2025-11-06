package com.team19.musuimsa.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team19.musuimsa.exception.notfound.ShelterNotFoundException;
import com.team19.musuimsa.notification.domain.ReviewReminderTask;
import com.team19.musuimsa.notification.repository.ReviewReminderTaskRepository;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewReminderServiceTest {

    @InjectMocks
    private ReviewReminderService reviewReminderService;

    @Mock
    private ReviewReminderTaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ShelterRepository shelterRepository;
    @Mock
    private FcmService fcmService;

    private User loginUser;
    private Shelter shelter;

    @BeforeEach
    void setUp() {
        loginUser = new User("user@example.com", "pw", "testUser", null);
        ReflectionTestUtils.setField(loginUser, "userId", 1L);

        shelter = Shelter.builder().shelterId(10L).name("테스트 쉼터").build();
    }

    @Test
    @DisplayName("도착 알림 시 10분 뒤 알림 작업이 DB에 저장된다")
    void scheduleReviewReminder_savesTask_with10MinuteDelay() {
        // given
        when(userRepository.getReferenceById(1L)).thenReturn(loginUser);
        when(shelterRepository.findById(10L)).thenReturn(Optional.of(shelter));

        ArgumentCaptor<ReviewReminderTask> taskCaptor = ArgumentCaptor.forClass(
                ReviewReminderTask.class);

        // when
        reviewReminderService.scheduleReviewReminder(10L, loginUser);

        // then
        verify(taskRepository).save(taskCaptor.capture());
        ReviewReminderTask savedTask = taskCaptor.getValue();

        LocalDateTime expectedNotifyAt = LocalDateTime.now().plusMinutes(10);

        assertSoftly(softly -> {
            softly.assertThat(savedTask.getUser()).isEqualTo(loginUser);
            softly.assertThat(savedTask.getShelter()).isEqualTo(shelter);
            softly.assertThat(savedTask.isSent()).isFalse();
            // 10분 뒤 시간인지 검증 (테스트 실행 시간을 고려해 10초 이내 오차 허용)
            softly.assertThat(savedTask.getNotifyAt()).isCloseTo(expectedNotifyAt,
                    org.assertj.core.api.Assertions.within(10,
                            java.time.temporal.ChronoUnit.SECONDS));
        });
    }

    @Test
    @DisplayName("도착 알림 시 쉼터가 없으면 ShelterNotFoundException 발생")
    void scheduleReviewReminder_throwsException_whenShelterNotFound() {
        // given
        when(userRepository.getReferenceById(1L)).thenReturn(loginUser);
        when(shelterRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewReminderService.scheduleReviewReminder(99L, loginUser))
                .isInstanceOf(ShelterNotFoundException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("처리할 알림이 없으면 FCM을 호출하지 않는다")
    void processPendingReminders_doesNothing_whenNoTasks() {
        // given
        when(taskRepository.findPendingTasks(any(LocalDateTime.class))).thenReturn(
                Collections.emptyList());

        // when
        reviewReminderService.processPendingReminders();

        // then
        //
        verify(fcmService, never()).sendPushNotification(anyLong(), anyString(), anyString(),
                anyMap());
    }

    @Test
    @DisplayName("보낼 알림이 있으면 FCM 전송 후 작업을 'sent'로 표시한다")
    void processPendingReminders_sendsFcm_andMarksAsSent() {
        // given
        ReviewReminderTask realTask = new ReviewReminderTask(loginUser, shelter,
                LocalDateTime.now().minusMinutes(1));
        ReviewReminderTask task = Mockito.spy(realTask);

        when(taskRepository.findPendingTasks(any(LocalDateTime.class))).thenReturn(List.of(task));
        //
        when(fcmService.sendPushNotification(eq(1L), anyString(), anyString(),
                any(Map.class)))
                .thenReturn(true);

        // when
        reviewReminderService.processPendingReminders();

        // then
        //
        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fcmService, times(1)).sendPushNotification(
                eq(1L),
                eq("테스트 쉼터 이용은 어떠셨나요?"),
                eq("10분 전 방문하신 쉼터의 소중한 리뷰를 남겨주세요!"),
                dataCaptor.capture() //
        );

        //
        assertThat(dataCaptor.getValue())
                .containsEntry("type", "REVIEW_REMINDER")
                .containsEntry("shelterId", "10");

        verify(task, times(1)).markAsSent();
        verify(taskRepository, times(1)).save(task);
        assertThat(task.isSent()).isTrue();
    }

    @Test
    @DisplayName("FCM 전송이 실패(false)해도 작업을 'sent'로 표시한다 (중복 방지)")
    void processPendingReminders_marksAsSent_evenIfFcmFails() {
        // given
        ReviewReminderTask realTask = new ReviewReminderTask(loginUser, shelter,
                LocalDateTime.now().minusMinutes(1));
        ReviewReminderTask task = Mockito.spy(realTask);

        when(taskRepository.findPendingTasks(any(LocalDateTime.class))).thenReturn(List.of(task));
        //
        when(fcmService.sendPushNotification(eq(1L), anyString(), anyString(),
                any(Map.class)))
                .thenReturn(false);

        // when
        reviewReminderService.processPendingReminders();

        // then
        //
        verify(fcmService, times(1)).sendPushNotification(anyLong(), anyString(), anyString(),
                any(Map.class));

        // 실패했음에도 불구하고,
        verify(task, times(1)).markAsSent(); // markAsSent 호출
        verify(taskRepository, times(1)).save(task); // save 호출
        assertThat(task.isSent()).isTrue(); // 상태가 true로 변경됨
    }
}