import { Routes } from '@angular/router';

export const PROFILE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/profile-view/profile-view.component').then((m) => m.ProfileViewComponent),
  },
];
