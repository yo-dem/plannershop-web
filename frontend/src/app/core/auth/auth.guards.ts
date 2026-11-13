import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Role } from './auth.models';
import { AuthService } from './auth.service';

/**
 * Pages reserved to signed-in users. With a temporary password, the only
 * reachable page is the password change.
 *
 * Note: guards are for the UI; the real security is in the backend.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.user();
  if (!user) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  if (user.mustChangePassword && state.url !== '/change-password') {
    return router.createUrlTree(['/change-password']);
  }
  return true;
};

/** Pages reserved to some roles; everyone else goes back to the home page. */
export const roleGuard =
  (...roles: Role[]): CanActivateFn =>
  () =>
    inject(AuthService).hasRole(...roles) || inject(Router).createUrlTree(['/']);

/** The sign-in page is not needed by whoever is already signed in. */
export const guestGuard: CanActivateFn = () =>
  !inject(AuthService).authenticated() || inject(Router).createUrlTree(['/']);
