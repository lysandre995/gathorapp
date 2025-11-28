package com.alfano.gathorapp.pattern.observer;

import com.alfano.gathorapp.notification.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer Pattern - Subject Implementation.
 * 
 * Manages a list of observers and notifies them when new notifications are
 * created.
 * Uses CopyOnWriteArrayList for thread-safe operations.
 * 
 * DESIGN PATTERN: OBSERVER
 * - Subject: NotificationManager
 * - Observers: PersistenceNotificationObserver, WebSocketNotificationObserver
 * - Purpose: Decouple notification creation from delivery mechanisms
 */
@Component
@Slf4j
public class NotificationManager implements NotificationSubject {

    /**
     * Thread-safe list of observers.
     * CopyOnWriteArrayList is ideal for:
     * - Few write operations (observer registration/removal)
     * - Many read operations (observer notifications)
     * - Concurrent access without explicit synchronization
     */
    private final List<NotificationObserver> observers = new CopyOnWriteArrayList<>();

    @Override
    public void registerObserver(NotificationObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            log.info("Registered observer: {}", observer.getName());
        }
    }

    @Override
    public void removeObserver(NotificationObserver observer) {
        if (observers.remove(observer)) {
            log.info("Removed observer: {}", observer.getName());
        }
    }

    @Override
    public void notifyObservers(Notification notification) {
        log.debug("Notifying {} observers for notification type: {}",
                observers.size(), notification.getType());

        // Notifica tutti gli observers in parallelo per evitare blocking
        observers.parallelStream().forEach(observer -> {
            try {
                observer.onNotification(notification);
                log.debug("Observer {} notified successfully", observer.getName());
            } catch (Exception e) {
                // Log error ma non fermare gli altri observers
                log.error("Error notifying observer {}: {}",
                        observer.getName(), e.getMessage(), e);
            }
        });
    }

    /**
     * Retrieves the number of registered observers.
     * Useful for monitoring and testing.
     */
    public int getObserverCount() {
        return observers.size();
    }

    /**
     * Retrieves the list of names of the registered observers.
     */
    public List<String> getObserverNames() {
        return observers.stream()
                .map(NotificationObserver::getName)
                .toList();
    }
}
