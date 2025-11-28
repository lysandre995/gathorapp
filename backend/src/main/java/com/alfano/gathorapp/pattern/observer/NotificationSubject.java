package com.alfano.gathorapp.pattern.observer;

import com.alfano.gathorapp.notification.Notification;

/**
 * Observer Pattern - Subject Interface.
 * 
 * The subject maintains a list of observers and notifies them
 * when a new notification is created.
 */
public interface NotificationSubject {

    /**
     * Registers an observer to receive notifications.
     * 
     * @param observer the observer to register
     */
    void registerObserver(NotificationObserver observer);

    /**
     * Removes an observer.
     * 
     * @param observer the observer to remove
     */
    void removeObserver(NotificationObserver observer);

    /**
     * Notifies all registered observers.
     * 
     * @param notification the notification to send
     */
    void notifyObservers(Notification notification);
}
