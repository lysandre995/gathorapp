import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

// Angular Material
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatBadgeModule } from '@angular/material/badge';

import { OutingResponse } from '../../../../generated/model/outingResponse';

@Component({
  selector: 'app-outing-card',
  standalone: true,
  imports: [
    DatePipe,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatBadgeModule,
  ],
  template: `
    <mat-card class="outing-card">
      <div class="placeholder-image">
        <mat-icon>explore</mat-icon>
      </div>

      <mat-card-header>
        <mat-card-title>{{ outing().title }}</mat-card-title>
        <mat-card-subtitle>
          <div class="organizer">
            <mat-icon>person</mat-icon>
            <span>{{ outing().organizer?.name || 'Unknown' }}</span>
          </div>
          @if (outing().event) {
          <div class="event-link">
            <mat-icon>event</mat-icon>
            <span>{{ outing().event?.title }}</span>
          </div>
          }
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <p class="description">{{ outing().description }}</p>

        <div class="outing-info">
          <div class="info-item">
            <mat-icon>calendar_today</mat-icon>
            <span>{{ outing().outingDate | date : 'dd/MM/yyyy HH:mm' }}</span>
          </div>

          <div class="info-item">
            <mat-icon>location_on</mat-icon>
            <span>{{ outing().location }}</span>
          </div>

          @if (outing().maxParticipants) {
          <div class="info-item">
            <mat-icon>group</mat-icon>
            <span>Max {{ outing().maxParticipants }} participants</span>
          </div>
          }

          @if (outing().latitude && outing().longitude) {
          <div class="info-item">
            <mat-icon>place</mat-icon>
            <span>{{ outing().latitude?.toFixed(4) }}, {{ outing().longitude?.toFixed(4) }}</span>
          </div>
          }
        </div>
      </mat-card-content>

      <mat-card-actions>
        <button mat-raised-button color="primary" [routerLink]="['/outings', outing().id]">
          <mat-icon>visibility</mat-icon>
          View Details
        </button>
      </mat-card-actions>
    </mat-card>
  `,
  styles: [
    `
      .outing-card {
        height: 100%;
        display: flex;
        flex-direction: column;
        transition: transform 0.2s, box-shadow 0.2s;

        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
        }
      }

      .placeholder-image {
        height: 200px;
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        display: flex;
        align-items: center;
        justify-content: center;

        mat-icon {
          font-size: 64px;
          width: 64px;
          height: 64px;
          color: white;
        }
      }

      mat-card-header {
        margin: 16px;
      }

      .organizer,
      .event-link {
        display: flex;
        align-items: center;
        gap: 4px;
        margin-top: 4px;

        mat-icon {
          font-size: 16px;
          width: 16px;
          height: 16px;
        }
      }

      .event-link {
        color: #1976d2;
        font-style: italic;
      }

      mat-card-content {
        flex: 1;
        padding: 0 16px;
      }

      .description {
        margin: 0 0 16px 0;
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;
        text-overflow: ellipsis;
        line-height: 1.5;
      }

      .outing-info {
        display: flex;
        flex-direction: column;
        gap: 8px;
        margin-bottom: 16px;
      }

      .info-item {
        display: flex;
        align-items: center;
        gap: 8px;
        color: rgba(0, 0, 0, 0.6);
        font-size: 14px;

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }

      mat-card-actions {
        padding: 8px 16px 16px;
        display: flex;
        gap: 8px;
        justify-content: space-between;
      }
    `,
  ],
})
export class OutingCardComponent {
  outing = input.required<OutingResponse>();
}
