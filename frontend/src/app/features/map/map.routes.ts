import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/guards/auth.guard';

export const MAP_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/map-view/map-view.component').then((m) => m.MapViewComponent),
    canActivate: [authGuard],
  },
];
