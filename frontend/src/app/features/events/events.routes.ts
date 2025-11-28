import { Routes } from '@angular/router';

export const EVENTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/event-list/event-list.component').then((m) => m.EventListComponent),
  },
  {
    path: 'create',
    loadComponent: () =>
      import('./pages/event-create/event-create.component').then((m) => m.EventCreateComponent),
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./pages/event-create/event-create.component').then((m) => m.EventCreateComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/event-detail/event-detail.component').then((m) => m.EventDetailComponent),
  },
];
