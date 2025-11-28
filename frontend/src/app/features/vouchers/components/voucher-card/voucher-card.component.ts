import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';

// Angular Material
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';

import { VoucherResponse } from '../../../../generated/model/voucherResponse';

@Component({
  selector: 'app-voucher-card',
  standalone: true,
  imports: [DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatChipsModule],
  template: `
    <mat-card class="voucher-card" [class.redeemed]="voucher().status === 'REDEEMED'">
      <mat-card-header>
        <mat-card-title>{{ voucher().reward?.title || 'Reward' }}</mat-card-title>
        <mat-card-subtitle>
          <mat-chip [class]="'status-' + voucher().status?.toLowerCase()">
            {{ voucher().status }}
          </mat-chip>
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <p class="description">{{ voucher().reward?.description }}</p>

        <div class="voucher-info">
          @if (voucher().reward?.business) {
          <div class="info-item">
            <mat-icon>business</mat-icon>
            <span>Business: {{ voucher().reward?.business?.name }}</span>
          </div>
          }

          <div class="info-item">
            <mat-icon>calendar_today</mat-icon>
            <span>Issued: {{ voucher().issuedAt | date : 'short' }}</span>
          </div>

          @if (voucher().expiresAt) {
          <div class="info-item" [class.expired]="isExpired()">
            <mat-icon>schedule</mat-icon>
            <span>Expires: {{ voucher().expiresAt | date : 'short' }}</span>
          </div>
          }

          @if (voucher().redeemedAt) {
          <div class="info-item">
            <mat-icon>check_circle</mat-icon>
            <span>Redeemed: {{ voucher().redeemedAt | date : 'short' }}</span>
          </div>
          }
        </div>

        @if (voucher().qrCode && voucher().status === 'ACTIVE') {
        <div class="qr-code">
          <mat-icon>qr_code_2</mat-icon>
          <p>QR Code: {{ voucher().qrCode }}</p>
        </div>
        }
      </mat-card-content>

      <mat-card-actions>
        @if (voucher().status === 'ACTIVE' && !isExpired()) {
        <button mat-raised-button color="primary" (click)="viewQr.emit(voucher().qrCode!)">
          <mat-icon>qr_code</mat-icon>
          Show QR Code
        </button>
        }
      </mat-card-actions>
    </mat-card>
  `,
  styles: [
    `
      .voucher-card {
        height: 100%;
        display: flex;
        flex-direction: column;
        transition: transform 0.2s, box-shadow 0.2s;

        &:hover:not(.redeemed) {
          transform: translateY(-4px);
          box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
        }

        &.redeemed {
          opacity: 0.7;
          background-color: #f5f5f5;
        }
      }

      mat-card-header {
        margin: 16px;
      }

      .status-active {
        background-color: #4caf50;
        color: white;
      }

      .status-redeemed {
        background-color: #9e9e9e;
        color: white;
      }

      .status-expired {
        background-color: #f44336;
        color: white;
      }

      .status-cancelled {
        background-color: #ff9800;
        color: white;
      }

      mat-card-content {
        flex: 1;
        padding: 0 16px;
      }

      .description {
        margin: 0 0 16px 0;
        line-height: 1.5;
      }

      .voucher-info {
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

        &.expired {
          color: #f44336;
        }

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }

      .qr-code {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px;
        background-color: #e3f2fd;
        border-radius: 8px;
        margin-top: 16px;

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
          color: #1976d2;
        }

        p {
          margin: 0;
          font-family: monospace;
          font-size: 12px;
          color: #1976d2;
        }
      }

      mat-card-actions {
        padding: 8px 16px 16px;
        display: flex;
        gap: 8px;
      }
    `,
  ],
})
export class VoucherCardComponent {
  voucher = input.required<VoucherResponse>();
  viewQr = output<string>();

  isExpired(): boolean {
    const expiresAt = this.voucher().expiresAt;
    if (!expiresAt) return false;
    return new Date(expiresAt) < new Date();
  }
}
