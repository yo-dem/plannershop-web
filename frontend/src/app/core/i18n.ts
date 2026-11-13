/**
 * Language of the user interface.
 *
 * Texts are written in English in the code and marked for translation
 * (i18n attributes in templates, $localize in TypeScript). The translations
 * are in src/locale/messages.<language>.json and are loaded at start-up by
 * main.ts, before the application is created.
 */
export const LANGUAGE = 'it';

/** Locale for dates, numbers and currencies (date, number and currency pipes). */
export const LOCALE = 'it-IT';

/** Month names in the UI language, January first ("gennaio", "febbraio", ...). */
export function monthNames(locale = LOCALE): string[] {
  const format = new Intl.DateTimeFormat(locale, { month: 'long' });
  return Array.from({ length: 12 }, (_, i) => format.format(new Date(2024, i, 1)));
}

/** First letter upper case ("gennaio" → "Gennaio"). */
export function capitalize(s: string): string {
  return s ? s[0].toUpperCase() + s.slice(1) : s;
}
