package com.alfano.gathorapp.pattern.observer;

import com.alfano.gathorapp.notification.Notification;

/**
 * Observer Pattern - Observer Interface.
 * 
 * Observers are notified when a new notification is created.
 * Different implementations handle notifications in different ways:
 * - Persistence: saves to the database
 * - WebSocket: sends in real time
 * - Email: sends via email (future)
 * - Push: sends a push notification (future)
 */
public interface NotificationObserver {

    /**
     * Called when a new notification is created.
     * 
     * @param notification the notification to handle
     */
    void onNotification(Notification notification);

    /**
     * Returns the name of this observer (for logging/debugging).
     * 
     * @return the observer's name
     */
    String getName();
}
