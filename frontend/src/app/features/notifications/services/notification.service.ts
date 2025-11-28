import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap, catchError, of, interval } from 'rxjs';
import { NotificationsService } from '../../../generated/api/notifications.service';
import { NotificationResponse } from '../../../generated/model/notificationResponse';

/**
 * Service for managing notifications with polling
 * Wraps the generated OpenAPI NotificationsService
 */
@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private notificationsApi = inject(NotificationsService);

  notifications = signal<NotificationResponse[]>([]);
  unreadCount = signal<number>(0);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  private pollingInterval = 30000; // Poll every 30 seconds
  private pollingSubscription: any = null;

  /**
   * Get all notifications for authenticated user
   */
  getNotifications(): Observable<NotificationResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.notificationsApi.getNotifications().pipe(
      tap({
        next: (notifications: NotificationResponse[]) => {
          this.notifications.set(notifications);
          this.updateUnreadCount(notifications);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading notifications');
          this.loading.set(false);
          console.error('Error loading notifications:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error loading notifications');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Get unread notifications only
   */
  getUnreadNotifications(): Observable<NotificationResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.notificationsApi.getUnreadNotifications().pipe(
      tap({
        next: (notifications) => {
          this.notifications.set(notifications);
          this.updateUnreadCount(notifications);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading unread notifications');
          this.loading.set(false);
          console.error('Error loading unread notifications:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error loading unread notifications');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Mark notification as read
   * @param notificationId Notification ID to mark as read
   */
  markAsRead(notificationId: string): Observable<NotificationResponse> {
    return this.notificationsApi.markAsRead(notificationId).pipe(
      tap({
        next: (updatedNotification) => {
          // Update the notification in the list
          this.notifications.update((notifications) =>
            notifications.map((n) => (n.id === notificationId ? updatedNotification : n))
          );
          // Recalculate unread count
          this.updateUnreadCount(this.notifications());
        },
        error: (err) => {
          console.error('Error marking notification as read:', err);
        },
      })
    );
  }

  /**
   * Mark all notifications as read
   */
  markAllAsRead(): Observable<any> {
    return this.notificationsApi.markAllAsRead().pipe(
      tap({
        next: () => {
          // Update all notifications to read
          this.notifications.update((notifications) =>
            notifications.map((n) => ({ ...n, read: true }))
          );
          this.unreadCount.set(0);
        },
        error: (err) => {
          console.error('Error marking all as read:', err);
        },
      })
    );
  }

  /**
   * Delete a notification
   * @param notificationId Notification ID to delete
   */
  deleteNotification(notificationId: string): Observable<any> {
    return this.notificationsApi.deleteNotification(notificationId).pipe(
      tap({
        next: () => {
          // Remove from list
          this.notifications.update((notifications) =>
            notifications.filter((n) => n.id !== notificationId)
          );
          // Recalculate unread count
          this.updateUnreadCount(this.notifications());
        },
        error: (err) => {
          console.error('Error deleting notification:', err);
        },
      })
    );
  }

  /**
   * Start polling for new notifications
   */
  startPolling() {
    if (this.pollingSubscription) {
      return; // Already polling
    }

    // Initial load
    this.getNotifications().subscribe();

    // Poll every 30 seconds
    this.pollingSubscription = interval(this.pollingInterval).subscribe(() => {
      this.getNotifications().subscribe();
    });
  }

  /**
   * Stop polling for notifications
   */
  stopPolling() {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = null;
    }
  }

  /**
   * Update unread count from notification list
   */
  private updateUnreadCount(notifications: NotificationResponse[]) {
    const count = notifications.filter((n) => !n.read).length;
    this.unreadCount.set(count);
  }

  /**
   * Get unread count
   */
  getUnreadCount(): number {
    return this.unreadCount();
  }
}
