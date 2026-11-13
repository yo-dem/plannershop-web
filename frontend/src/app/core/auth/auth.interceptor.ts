import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';

import { LANGUAGE } from '../i18n';
import { AuthService } from './auth.service';

const AUTH_APIS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout'];

/**
 * For every /api call:
 * 1. adds the Authorization header with the access token, and Accept-Language,
 *    so the backend answers with messages in the language of the UI;
 * 2. if the server answers 401 (expired token), renews the session and retries
 *    the request once; if the session cannot be renewed, goes back to sign-in.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/')) return next(req);

  const auth = inject(AuthService);
  const authCall = AUTH_APIS.some((u) => req.url.startsWith(u));
  const withHeaders = (r: HttpRequest<unknown>) => {
    const token = auth.token();
    const headers: Record<string, string> = { 'Accept-Language': LANGUAGE };
    if (token && !authCall) headers['Authorization'] = `Bearer ${token}`;
    return r.clone({ setHeaders: headers });
  };

  return next(withHeaders(req)).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401 || authCall) {
        return throwError(() => err);
      }
      return from(auth.refresh()).pipe(
        switchMap((ok) => {
          if (ok) return next(withHeaders(req));
          auth.sessionExpired();
          return throwError(() => err);
        }),
      );
    }),
  );
};
