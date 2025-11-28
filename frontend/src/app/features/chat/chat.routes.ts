import { Routes } from '@angular/router';

export const CHAT_ROUTES: Routes = [
  {
    path: 'outing/:outingId',
    loadComponent: () =>
      import('./pages/chat-view/chat-view.component').then((m) => m.ChatViewComponent),
  },
];
