import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Spazi tolti e lettere in maiuscolo. */
export function normalizzaIban(iban: string): string {
  return iban.replace(/\s/g, '').toUpperCase();
}

/**
 * IBAN valido: paese, cifre di controllo, 11–30 caratteri alfanumerici e
 * controllo Mod-97 uguale a 1 (stessa verifica del backend, SPECIFICA §7.4).
 */
export function ibanValido(iban: string): boolean {
  const s = normalizzaIban(iban);
  if (!/^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/.test(s)) return false;
  let resto = 0;
  for (const c of s.slice(4) + s.slice(0, 4)) {
    const cifre = /[A-Z]/.test(c) ? String(c.charCodeAt(0) - 55) : c; // A = 10 ... Z = 35
    for (const d of cifre) resto = (resto * 10 + Number(d)) % 97;
  }
  return resto === 1;
}

/** In gruppi di quattro, per leggerlo meglio: "IT60 X054 2811 ...". */
export function formattaIban(iban: string | null): string {
  return iban ? normalizzaIban(iban).replace(/(.{4})(?=.)/g, '$1 ') : '';
}

/** Validatore del modulo: campo vuoto valido, altrimenti errore "iban". */
export function ibanValidator(c: AbstractControl<string>): ValidationErrors | null {
  return c.value && !ibanValido(c.value) ? { iban: true } : null;
}
