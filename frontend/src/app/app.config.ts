import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeIt from '@angular/common/locales/it';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { traduzioniPrimeNG } from './core/primeng-it';

// Formati italiani per date, numeri e valute (pipe date, number, currency).
registerLocaleData(localeIt);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    // Prima di mostrare qualsiasi pagina prova a ripristinare la sessione dal cookie.
    provideAppInitializer(() => inject(AuthService).refresh()),
    { provide: LOCALE_ID, useValue: 'it-IT' },
    providePrimeNG({
      theme: {
        preset: Aura,
        // Il tema scuro si attiva aggiungendo la classe "app-dark" a <html>.
        options: { darkModeSelector: '.app-dark' },
      },
      translation: traduzioniPrimeNG,
    }),
    MessageService,
    ConfirmationService,
  ],
};
