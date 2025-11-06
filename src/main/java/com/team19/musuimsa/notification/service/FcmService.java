package com.team19.musuimsa.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.team19.musuimsa.user.domain.UserDevice;
import com.team19.musuimsa.user.repository.UserDeviceRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final UserDeviceRepository userDeviceRepository;
    private final ResourceLoader resourceLoader;

    @Value("${fcm.service-account-key-path}")
    private String serviceAccountKeyPath;

    @PostConstruct
    public void initialize() {
        try {
            Resource resource = resourceLoader.getResource(serviceAccountKeyPath);
            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase app has been initialized from: {}", serviceAccountKeyPath);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase app from path: {}", serviceAccountKeyPath, e);
        }
    }

    public boolean sendPushNotification(Long userId, String title, String body) {
        List<UserDevice> devices = userDeviceRepository.findByUser_UserId(userId);
        if (devices.isEmpty()) {
            return false;
        }

        boolean atLeastOneSuccess = false;
        for (UserDevice device : devices) {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(device.getDeviceToken())
                    .setNotification(notification)
                    .putData("click_action", "SHOW_NEARBY_SHELTERS")
                    .build();

            try {
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Successfully sent message to token {}: {}",
                        maskToken(device.getDeviceToken()),
                        response);
                atLeastOneSuccess = true;
            } catch (Exception e) {
                log.error("Failed to send push notification to token {}: {}",
                        maskToken(device.getDeviceToken()), e.getMessage());
            }
        }

        return atLeastOneSuccess;
    }

    public boolean sendPushNotification(Long userId, String title, String body,
            Map<String, String> data) {
        List<UserDevice> devices = userDeviceRepository.findByUser_UserId(userId);
        if (devices.isEmpty()) {
            log.warn("User {} has no registered devices. Skipping push.", userId);
            return false;
        }

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        boolean atLeastOneSuccess = false;
        for (UserDevice device : devices) {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(device.getDeviceToken())
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data); //
            }

            try {
                String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
                log.info("Successfully sent message to token {}: {}",
                        maskToken(device.getDeviceToken()),
                        response);
                atLeastOneSuccess = true;
            } catch (Exception e) {
                log.error("Failed to send push notification to token {}: {}",
                        maskToken(device.getDeviceToken()), e.getMessage());
            }
        }

        return atLeastOneSuccess;
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 16) {
            return "****";
        }

        return token.substring(0, 8) + "..." + token.substring(token.length() - 8);
    }
}