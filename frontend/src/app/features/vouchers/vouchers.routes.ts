import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/guards/auth.guard';

export const VOUCHERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/voucher-list/voucher-list.component').then((m) => m.VoucherListComponent),
    canActivate: [authGuard],
  },
];
