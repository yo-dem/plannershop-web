import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Ruolo } from './auth.models';
import { AuthService } from './auth.service';

/**
 * Pagine riservate agli utenti collegati. Se la password è provvisoria,
 * l'unica pagina accessibile è quella di cambio password.
 *
 * Nota: le guard servono all'interfaccia; la sicurezza vera è nel backend.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const utente = auth.utente();
  if (!utente) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  if (utente.deveCambiarePassword && state.url !== '/cambia-password') {
    return router.createUrlTree(['/cambia-password']);
  }
  return true;
};

/** Pagine riservate a certi ruoli; gli altri tornano alla home. */
export const ruoloGuard =
  (...ruoli: Ruolo[]): CanActivateFn =>
  () =>
    inject(AuthService).haRuolo(...ruoli) || inject(Router).createUrlTree(['/']);

/** La pagina di login non serve a chi è già collegato. */
export const ospiteGuard: CanActivateFn = () =>
  !inject(AuthService).autenticato() || inject(Router).createUrlTree(['/']);
