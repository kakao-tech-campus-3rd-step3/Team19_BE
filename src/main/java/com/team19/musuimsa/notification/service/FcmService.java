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

    public void sendPushNotification(Long userId, String title, String body) {
        List<UserDevice> devices = userDeviceRepository.findByUser_UserId(userId);

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
                log.info("Successfully sent message to token {}: {}", device.getDeviceToken(),
                        response);
            } catch (Exception e) {
                log.error("Failed to send push notification to token {}: {}",
                        device.getDeviceToken(), e.getMessage());
            }
        }
    }
}