import { Routes } from '@angular/router';

import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guards';

const title = (page: string) => `${page} · PlannerShop`;

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
    title: title($localize`:@@title.login:Sign in`),
  },
  {
    // All the pages below require sign-in.
    path: '',
    canActivateChild: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/home/home').then((m) => m.Home),
        title: 'PlannerShop',
      },
      {
        path: 'change-password',
        loadComponent: () => import('./pages/change-password/change-password').then((m) => m.ChangePassword),
        title: title($localize`:@@title.changePassword:Change password`),
      },
      {
        path: 'customers',
        canActivate: [roleGuard('ADMIN', 'OPERATOR')],
        loadComponent: () => import('./pages/customers/customers').then((m) => m.Customers),
        title: title($localize`:@@title.customers:Customers`),
      },
      {
        path: 'customers/birthdays',
        canActivate: [roleGuard('ADMIN', 'OPERATOR')],
        loadComponent: () => import('./pages/customers/birthdays').then((m) => m.Birthdays),
        title: title($localize`:@@title.birthdays:Birthdays`),
      },
      {
        path: 'suppliers',
        canActivate: [roleGuard('ADMIN', 'OPERATOR')],
        loadComponent: () => import('./pages/suppliers/suppliers').then((m) => m.Suppliers),
        title: title($localize`:@@title.suppliers:Suppliers`),
      },
      {
        path: 'products',
        canActivate: [roleGuard('ADMIN', 'OPERATOR')],
        loadComponent: () => import('./pages/products/products').then((m) => m.Products),
        title: title($localize`:@@title.products:Products`),
      },
      {
        path: 'services',
        canActivate: [roleGuard('ADMIN', 'OPERATOR')],
        loadComponent: () => import('./pages/services/services').then((m) => m.Services),
        title: title($localize`:@@title.services:Services`),
      },
      {
        path: 'platform',
        canActivate: [roleGuard('SUPERADMIN')],
        loadComponent: () => import('./pages/platform/tenants').then((m) => m.Tenants),
        title: title($localize`:@@title.tenants:Businesses`),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
