package com.alfano.gathorapp.pattern.observer;

import com.alfano.gathorapp.notification.Notification;
import com.alfano.gathorapp.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Observer Pattern - Concrete Observer.
 * 
 * This observer persists notifications to the database so users can
 * view their notification history even when offline.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PersistenceNotificationObserver implements NotificationObserver {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNotification(Notification notification) {
        try {
            Notification saved = notificationRepository.save(notification);
            log.debug("Persisted notification {} to database", saved.getId());
        } catch (Exception e) {
            log.error("Failed to persist notification: {}", e.getMessage(), e);
            // Don't throw - let other observers continue
        }
    }

    @Override
    public String getName() {
        return "PersistenceNotificationObserver";
    }
}
