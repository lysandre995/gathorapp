import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';
import { MainLayoutComponent } from './shared/components/layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      {
        path: '',
        redirectTo: 'events',
        pathMatch: 'full',
      },
      {
        path: 'auth',
        loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
      },
      {
        path: 'events',
        loadChildren: () => import('./features/events/events.routes').then((m) => m.EVENTS_ROUTES),
        canActivate: [authGuard],
      },
      {
        path: 'outings',
        loadChildren: () =>
          import('./features/outings/outings.routes').then((m) => m.OUTINGS_ROUTES),
        canActivate: [authGuard],
      },
      {
        path: 'notifications',
        loadChildren: () =>
          import('./features/notifications/notifications.routes').then(
            (m) => m.NOTIFICATIONS_ROUTES
          ),
        canActivate: [authGuard],
      },
      {
        path: 'profile',
        loadChildren: () =>
          import('./features/profile/profile.routes').then((m) => m.PROFILE_ROUTES),
        canActivate: [authGuard],
      },
      {
        path: 'chat',
        loadChildren: () => import('./features/chat/chat.routes').then((m) => m.CHAT_ROUTES),
        canActivate: [authGuard],
      },
      {
        path: 'map',
        loadChildren: () => import('./features/map/map.routes').then((m) => m.MAP_ROUTES),
        canActivate: [authGuard],
      },
      {
        path: 'vouchers',
        loadChildren: () =>
          import('./features/vouchers/vouchers.routes').then((m) => m.VOUCHERS_ROUTES),
        canActivate: [authGuard],
      },
      {
        path: 'admin',
        loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
        canActivate: [authGuard],
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'events',
  },
];
