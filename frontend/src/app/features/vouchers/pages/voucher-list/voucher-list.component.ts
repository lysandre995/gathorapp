import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

// Angular Material
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { VoucherService } from '../../services/voucher.service';
import { VoucherCardComponent } from '../../components/voucher-card/voucher-card.component';

@Component({
  selector: 'app-voucher-list',
  standalone: true,
  imports: [
    CommonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatButtonModule,
    MatDialogModule,
    MatSnackBarModule,
    VoucherCardComponent,
  ],
  template: `
    <div class="voucher-list-container">
      <header class="page-header">
        <div class="header-content">
          <div>
            <h1>My Vouchers</h1>
            <p class="subtitle">Rewards from your organized outings</p>
          </div>
          <mat-icon class="voucher-icon">confirmation_number</mat-icon>
        </div>
      </header>

      @if (voucherService.loading()) {
      <div class="loading">
        <mat-spinner></mat-spinner>
        <p>Loading vouchers...</p>
      </div>
      }

      @if (voucherService.error()) {
      <div class="error">
        <mat-icon>error_outline</mat-icon>
        <h3>Loading Error</h3>
        <p>{{ voucherService.error() }}</p>
        <button mat-raised-button color="primary" (click)="loadVouchers()">Retry</button>
      </div>
      }

      @if (!voucherService.loading() && !voucherService.error()) {
        @if (voucherService.vouchers().length > 0) {
      <div class="vouchers-grid">
        @for (voucher of voucherService.vouchers(); track voucher.id) {
        <app-voucher-card [voucher]="voucher" (viewQr)="showQrCode($event)" />
        }
      </div>
        } @else {
      <div class="empty-state">
        <mat-icon>confirmation_number</mat-icon>
        <h3>No Vouchers Yet</h3>
        <p>Organize outings with enough participants to earn rewards!</p>
        <p class="info-text">
          As a Premium user, you'll receive vouchers when your outings reach the required number
          of participants.
        </p>
      </div>
        }
      }
    </div>
  `,
  styles: [
    `
      .voucher-list-container {
        max-width: 1400px;
        margin: 0 auto;
        padding: 24px;
      }

      .page-header {
        margin-bottom: 32px;

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

          .voucher-icon {
            font-size: 64px;
            width: 64px;
            height: 64px;
            color: #1976d2;
          }
        }
      }

      .vouchers-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
        gap: 24px;
        margin-bottom: 32px;
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

        .info-text {
          max-width: 600px;
          font-size: 14px;
          color: #999;
          margin-top: 8px;
        }
      }

      .error mat-icon {
        color: #f44336;
      }

      @media (max-width: 768px) {
        .voucher-list-container {
          padding: 16px;
        }

        .vouchers-grid {
          grid-template-columns: 1fr;
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
export class VoucherListComponent implements OnInit {
  voucherService = inject(VoucherService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  ngOnInit() {
    this.loadVouchers();
  }

  loadVouchers() {
    this.voucherService.getMyVouchers().subscribe();
  }

  /**
   * Show QR code in dialog/snackbar
   * For a full implementation, you'd want a proper QR code dialog
   */
  showQrCode(qrCode: string) {
    // Simple implementation with snackbar
    // In production, use a dialog with actual QR code image
    this.snackBar.open(`QR Code: ${qrCode}`, 'Close', {
      duration: 10000,
      horizontalPosition: 'center',
      verticalPosition: 'top',
    });
  }
}
