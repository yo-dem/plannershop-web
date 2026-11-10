import { Routes } from '@angular/router';

import { authGuard, ospiteGuard, ruoloGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [ospiteGuard],
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
    title: 'Accedi · PlannerShop',
  },
  {
    // Tutte le pagine qui sotto richiedono l'accesso.
    path: '',
    canActivateChild: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/home/home').then((m) => m.Home),
        title: 'PlannerShop',
      },
      {
        path: 'cambia-password',
        loadComponent: () =>
          import('./pages/cambia-password/cambia-password').then((m) => m.CambiaPassword),
        title: 'Cambia password · PlannerShop',
      },
      {
        path: 'piattaforma',
        canActivate: [ruoloGuard('SUPERADMIN')],
        loadComponent: () => import('./pages/piattaforma/attivita').then((m) => m.Attivita),
        title: 'Attività · PlannerShop',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
