import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeIt from '@angular/common/locales/it';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';

import { routes } from './app.routes';

// Formati italiani per date, numeri e valute (pipe date, number, currency).
registerLocaleData(localeIt);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withFetch()),
    { provide: LOCALE_ID, useValue: 'it-IT' },
    providePrimeNG({
      theme: {
        preset: Aura,
        // Il tema scuro si attiva aggiungendo la classe "app-dark" a <html>.
        options: { darkModeSelector: '.app-dark' },
      },
    }),
  ],
};
