import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Spaces removed and letters upper-cased. */
export function normalizeIban(iban: string): string {
  return iban.replace(/\s/g, '').toUpperCase();
}

/**
 * Valid IBAN: country, check digits, 11–30 alphanumeric characters and Mod-97
 * check equal to 1 (same check as the backend, SPECIFICA §7.4).
 */
export function isValidIban(iban: string): boolean {
  const s = normalizeIban(iban);
  if (!/^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/.test(s)) return false;
  let remainder = 0;
  for (const c of s.slice(4) + s.slice(0, 4)) {
    const digits = /[A-Z]/.test(c) ? String(c.charCodeAt(0) - 55) : c; // A = 10 ... Z = 35
    for (const d of digits) remainder = (remainder * 10 + Number(d)) % 97;
  }
  return remainder === 1;
}

/** In groups of four, easier to read: "IT60 X054 2811 ...". */
export function formatIban(iban: string | null): string {
  return iban ? normalizeIban(iban).replace(/(.{4})(?=.)/g, '$1 ') : '';
}

/** Form validator: empty field is valid, otherwise "iban" error. */
export function ibanValidator(c: AbstractControl<string>): ValidationErrors | null {
  return c.value && !isValidIban(c.value) ? { iban: true } : null;
}
