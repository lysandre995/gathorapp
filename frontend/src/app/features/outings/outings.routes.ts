import { Routes } from '@angular/router';

export const OUTINGS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/outing-list/outing-list.component').then((m) => m.OutingListComponent),
  },
  {
    path: 'create',
    loadComponent: () =>
      import('./pages/outing-create/outing-create.component').then(
        (m) => m.OutingCreateComponent
      ),
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./pages/outing-create/outing-create.component').then(
        (m) => m.OutingCreateComponent
      ),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/outing-detail/outing-detail.component').then(
        (m) => m.OutingDetailComponent
      ),
  },
];
