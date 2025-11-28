import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

// Angular Material
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { NotificationService } from '../../services/notification.service';
import { NotificationItemComponent } from '../../components/notification-item/notification-item.component';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [
    CommonModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatBadgeModule,
    MatSnackBarModule,
    MatTabsModule,
    NotificationItemComponent,
  ],
  template: `
    <div class="notification-list-container">
      <header class="page-header">
        <div class="header-content">
          <div>
            <h1>Notifications</h1>
            <p class="subtitle">Stay updated with your activity</p>
          </div>
          <div class="header-actions">
            @if (notificationService.unreadCount() > 0) {
            <button mat-raised-button color="primary" (click)="markAllAsRead()">
              <mat-icon>done_all</mat-icon>
              Mark All as Read
            </button>
            }
            <button mat-icon-button (click)="refresh()" matTooltip="Refresh">
              <mat-icon>refresh</mat-icon>
            </button>
          </div>
        </div>
      </header>

      @if (notificationService.loading()) {
      <div class="loading">
        <mat-spinner></mat-spinner>
        <p>Loading notifications...</p>
      </div>
      }

      @if (notificationService.error()) {
      <div class="error">
        <mat-icon>error_outline</mat-icon>
        <h3>Loading Error</h3>
        <p>{{ notificationService.error() }}</p>
        <button mat-raised-button color="primary" (click)="refresh()">Retry</button>
      </div>
      }

      @if (!notificationService.loading() && !notificationService.error()) {
        @if (notificationService.notifications().length > 0) {
      <mat-tab-group>
        <mat-tab label="All ({{ notificationService.notifications().length }})">
          <mat-list class="notifications-list">
            @for (notification of notificationService.notifications(); track notification.id) {
            <app-notification-item
              [notification]="notification"
              (markAsRead)="onMarkAsRead($event)"
              (delete)="onDelete($event)"
            />
            }
          </mat-list>
        </mat-tab>

        <mat-tab label="Unread ({{ notificationService.unreadCount() }})">
          <mat-list class="notifications-list">
            @for (notification of getUnreadNotifications(); track notification.id) {
            <app-notification-item
              [notification]="notification"
              (markAsRead)="onMarkAsRead($event)"
              (delete)="onDelete($event)"
            />
            } @if (getUnreadNotifications().length === 0) {
            <div class="empty-state">
              <mat-icon>done_all</mat-icon>
              <p>No unread notifications</p>
            </div>
            }
          </mat-list>
        </mat-tab>
      </mat-tab-group>
        } @else {
      <div class="empty-state">
        <mat-icon>notifications_none</mat-icon>
        <h3>No Notifications</h3>
        <p>You're all caught up!</p>
      </div>
        }
      }
    </div>
  `,
  styles: [
    `
      .notification-list-container {
        max-width: 900px;
        margin: 24px auto;
        padding: 24px;
      }

      .page-header {
        margin-bottom: 24px;

        .header-content {
          display: flex;
          justify-content: space-between;
          align-items: center;

          h1 {
            margin: 0 0 8px 0;
            font-size: 32px;
            font-weight: 600;
            color: #333;
          }

          .subtitle {
            margin: 0;
            color: #666;
            font-size: 16px;
          }

          .header-actions {
            display: flex;
            align-items: center;
            gap: 12px;
          }
        }
      }

      .notifications-list {
        background-color: white;
        border-radius: 8px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        padding: 0;
      }

      .loading,
      .error,
      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 64px 24px;
        text-align: center;

        mat-icon {
          font-size: 64px;
          width: 64px;
          height: 64px;
          color: #999;
          margin-bottom: 16px;
        }

        h3 {
          margin: 0 0 8px 0;
          font-size: 24px;
          color: #333;
        }

        p {
          margin: 0 0 16px 0;
          color: #666;
        }
      }

      .error mat-icon {
        color: #f44336;
      }

      @media (max-width: 768px) {
        .notification-list-container {
          padding: 16px;
        }

        .page-header .header-content {
          flex-direction: column;
          align-items: flex-start;
          gap: 16px;
        }
      }
    `,
  ],
})
export class NotificationListComponent implements OnInit, OnDestroy {
  notificationService = inject(NotificationService);
  private snackBar = inject(MatSnackBar);

  ngOnInit() {
    // Start polling for notifications
    this.notificationService.startPolling();
  }

  ngOnDestroy() {
    // Stop polling when component is destroyed
    this.notificationService.stopPolling();
  }

  refresh() {
    this.notificationService.getNotifications().subscribe();
  }

  onMarkAsRead(notificationId: string) {
    this.notificationService.markAsRead(notificationId).subscribe({
      next: () => {
        this.snackBar.open('Marked as read', 'Close', { duration: 2000 });
      },
      error: (err) => {
        console.error('Error marking as read:', err);
        const message = err.error?.message || 'Error marking notification as read';
        this.snackBar.open(message, 'Close', { duration: 3000 });
      },
    });
  }

  onDelete(notificationId: string) {
    this.notificationService.deleteNotification(notificationId).subscribe({
      next: () => {
        this.snackBar.open('Notification deleted', 'Close', { duration: 2000 });
      },
      error: (err) => {
        console.error('Error deleting notification:', err);
        const message = err.error?.message || 'Error deleting notification';
        this.snackBar.open(message, 'Close', { duration: 3000 });
      },
    });
  }

  markAllAsRead() {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.snackBar.open('All notifications marked as read', 'Close', { duration: 2000 });
      },
      error: () => {
        this.snackBar.open('Error marking all as read', 'Close', { duration: 3000 });
      },
    });
  }

  getUnreadNotifications() {
    return this.notificationService.notifications().filter((n) => !n.read);
  }
}
