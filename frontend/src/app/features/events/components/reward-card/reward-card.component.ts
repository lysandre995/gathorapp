import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

// Angular Material
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';

import { RewardResponse } from '../../../../generated/model/rewardResponse';

@Component({
  selector: 'app-reward-card',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatChipsModule],
  template: `
    <mat-card class="reward-card">
      <mat-card-header>
        <div class="reward-icon">
          <mat-icon>card_giftcard</mat-icon>
        </div>
        <mat-card-title>{{ reward().title }}</mat-card-title>
        <mat-card-subtitle>
          <mat-chip class="participants-chip">
            <mat-icon>groups</mat-icon>
            {{ reward().requiredParticipants }} participants required
          </mat-chip>
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <p class="description">{{ reward().description }}</p>

        <div class="business-info">
          <mat-icon>business</mat-icon>
          <span>Offered by: {{ reward().business?.name || 'Business' }}</span>
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [
    `
      .reward-card {
        border-left: 4px solid #ff9800;
        background: linear-gradient(to right, #fff8e1 0%, #ffffff 20%);
        transition: transform 0.2s, box-shadow 0.2s;

        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
        }
      }

      mat-card-header {
        margin-bottom: 16px;

        .reward-icon {
          margin-right: 16px;

          mat-icon {
            font-size: 48px;
            width: 48px;
            height: 48px;
            color: #ff9800;
          }
        }
      }

      mat-card-title {
        font-size: 20px;
        font-weight: 600;
        color: #333;
      }

      mat-card-subtitle {
        margin-top: 8px;
      }

      .participants-chip {
        background-color: #ff9800;
        color: white;
        font-weight: 500;

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }

      .description {
        color: #666;
        line-height: 1.6;
        margin-bottom: 16px;
      }

      .business-info {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #666;
        font-size: 14px;

        mat-icon {
          font-size: 20px;
          width: 20px;
          height: 20px;
          color: #1976d2;
        }
      }
    `,
  ],
})
export class RewardCardComponent {
  reward = input.required<RewardResponse>();
}
