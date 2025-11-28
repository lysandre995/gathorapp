package com.alfano.gathorapp.pattern.observer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Initializes and registers all notification observers at application startup.
 * 
 * This is part of the Observer Pattern implementation.
 * All observers are registered with the NotificationManager on startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationObserverInitializer {

    private final NotificationManager notificationManager;
    private final WebSocketNotificationObserver webSocketObserver;
    private final PersistenceNotificationObserver persistenceObserver;

    @PostConstruct
    public void initialize() {
        log.info("Initializing notification observers...");

        // Register all observers
        notificationManager.registerObserver(persistenceObserver);
        notificationManager.registerObserver(webSocketObserver);

        log.info("Registered {} notification observers: {}",
                notificationManager.getObserverCount(),
                notificationManager.getObserverNames());
    }
}
