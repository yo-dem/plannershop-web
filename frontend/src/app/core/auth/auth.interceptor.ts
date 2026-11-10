import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

const API_AUTH = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout'];

/**
 * Per ogni chiamata /api:
 * 1. aggiunge l'header Authorization con l'access token;
 * 2. se il server risponde 401 (token scaduto), rinnova la sessione e
 *    ripete la richiesta una volta; se non si può rinnovare, torna al login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/')) return next(req);

  const auth = inject(AuthService);
  const chiamataAuth = API_AUTH.some((u) => req.url.startsWith(u));
  const conToken = (r: HttpRequest<unknown>) => {
    const token = auth.token();
    return token && !chiamataAuth ? r.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : r;
  };

  return next(conToken(req)).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401 || chiamataAuth) {
        return throwError(() => err);
      }
      return from(auth.refresh()).pipe(
        switchMap((ok) => {
          if (ok) return next(conToken(req));
          auth.sessioneScaduta();
          return throwError(() => err);
        }),
      );
    }),
  );
};
