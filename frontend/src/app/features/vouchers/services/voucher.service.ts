import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap, catchError, of } from 'rxjs';
import { VouchersService } from '../../../generated/api/vouchers.service';
import { VoucherResponse } from '../../../generated/model/voucherResponse';

/**
 * Service for managing vouchers
 * Wraps the generated OpenAPI VouchersService
 */
@Injectable({
  providedIn: 'root',
})
export class VoucherService {
  private vouchersApi = inject(VouchersService);

  vouchers = signal<VoucherResponse[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  /**
   * Get my vouchers (for authenticated user)
   */
  getMyVouchers(): Observable<VoucherResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.vouchersApi.getMyVouchers().pipe(
      tap({
        next: (vouchers) => {
          this.vouchers.set(vouchers);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading vouchers');
          this.loading.set(false);
          console.error('Error loading vouchers:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error loading vouchers');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Redeem a voucher by QR code
   * @param qrCode QR code to redeem
   */
  redeemVoucher(qrCode: string): Observable<VoucherResponse> {
    this.loading.set(true);
    this.error.set(null);

    return this.vouchersApi.redeemVoucher(qrCode).pipe(
      tap({
        next: () => {
          this.loading.set(false);
          // Refresh vouchers list
          this.getMyVouchers().subscribe();
        },
        error: (err) => {
          this.error.set('Error redeeming voucher');
          this.loading.set(false);
          console.error('Error redeeming voucher:', err);
        },
      })
    );
  }
}
