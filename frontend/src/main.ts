import { loadTranslations } from '@angular/localize';

import { LANGUAGE } from './app/core/i18n';
import it from './locale/messages.it.json';

// Translations must be loaded before any code that uses $localize runs:
// that is why the application is imported dynamically afterwards.
const translations: Record<string, Record<string, string>> = { it: it.translations };
if (translations[LANGUAGE]) {
  loadTranslations(translations[LANGUAGE]);
}
document.documentElement.lang = LANGUAGE;

import('./bootstrap').catch((err) => console.error(err));
