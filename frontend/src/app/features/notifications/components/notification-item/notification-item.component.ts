import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';

// Angular Material
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';

import { NotificationResponse } from '../../../../generated/model/notificationResponse';

@Component({
  selector: 'app-notification-item',
  standalone: true,
  imports: [DatePipe, MatListModule, MatIconModule, MatButtonModule, MatBadgeModule, MatTooltipModule],
  template: `
    <div class="notification-item" [class.unread]="!notification().read">
      <div class="notification-icon">
        <mat-icon [class]="'type-' + notification().type?.toLowerCase()">
          {{ getIconForType(notification().type) }}
        </mat-icon>
      </div>

      <div class="notification-content">
        <div class="notification-title">
          {{ notification().title }}
          @if (!notification().read) {
          <mat-icon class="unread-indicator">fiber_manual_record</mat-icon>
          }
        </div>

        <div class="notification-message">
          {{ notification().message }}
        </div>

        <div class="notification-time">{{ notification().createdAt | date : 'short' }}</div>
      </div>

      <div class="notification-actions">
        @if (!notification().read) {
        <button mat-icon-button (click)="onMarkAsRead($event)" matTooltip="Mark as read">
          <mat-icon>done</mat-icon>
        </button>
        }
        <button mat-icon-button (click)="onDelete($event)" matTooltip="Delete">
          <mat-icon>delete</mat-icon>
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .notification-item {
        display: flex;
        gap: 16px;
        align-items: flex-start;
        border-bottom: 1px solid rgba(0, 0, 0, 0.12);
        padding: 16px;
        min-height: 80px;
        transition: background-color 0.2s;

        &:hover {
          background-color: rgba(0, 0, 0, 0.04);
        }

        &.unread {
          background-color: #e3f2fd;

          &:hover {
            background-color: #bbdefb;
          }
        }
      }

      .notification-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;

          &.type-participation {
            color: #2196f3;
          }

          &.type-message {
            color: #4caf50;
          }

          &.type-reward {
            color: #ff9800;
          }

          &.type-system {
            color: #9e9e9e;
          }
        }
      }

      .notification-content {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 4px;
      }

      .notification-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 500;
        font-size: 16px;
        color: #333;

        .unread-indicator {
          font-size: 12px;
          width: 12px;
          height: 12px;
          color: #2196f3;
        }
      }

      .notification-message {
        color: #666;
        font-size: 14px;
        word-break: break-word;
      }

      .notification-time {
        font-size: 12px;
        color: #999;
        margin-top: 4px;
      }

      .notification-actions {
        display: flex;
        gap: 4px;
        flex-shrink: 0;
        align-items: flex-start;

        button {
          flex-shrink: 0;
        }
      }
    `,
  ],
})
export class NotificationItemComponent {
  notification = input.required<NotificationResponse>();
  markAsRead = output<string>();
  delete = output<string>();

  onMarkAsRead(event: Event) {
    event.stopPropagation();
    this.markAsRead.emit(this.notification().id!);
  }

  onDelete(event: Event) {
    event.stopPropagation();
    this.delete.emit(this.notification().id!);
  }

  getIconForType(type: string | undefined): string {
    switch (type) {
      case 'PARTICIPATION':
        return 'group_add';
      case 'MESSAGE':
        return 'chat';
      case 'REWARD':
        return 'card_giftcard';
      case 'SYSTEM':
        return 'notifications';
      default:
        return 'notifications';
    }
  }
}
