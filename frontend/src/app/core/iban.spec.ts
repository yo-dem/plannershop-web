import { describe, expect, it } from 'vitest';

import { formattaIban, ibanValido } from './iban';

describe('iban', () => {
  it('accetta IBAN validi anche con spazi e minuscole', () => {
    expect(ibanValido('IT60X0542811101000000123456')).toBe(true);
    expect(ibanValido(' it60 x054 2811 1010 0000 0123 456')).toBe(true);
    expect(ibanValido('DE89370400440532013000')).toBe(true);
  });

  it('rifiuta cifre di controllo e formati sbagliati', () => {
    expect(ibanValido('IT61X0542811101000000123456')).toBe(false);
    expect(ibanValido('IT60X05428')).toBe(false);
    expect(ibanValido('')).toBe(false);
  });

  it('formatta in gruppi di quattro', () => {
    expect(formattaIban('IT60X0542811101000000123456')).toBe('IT60 X054 2811 1010 0000 0123 456');
    expect(formattaIban(null)).toBe('');
  });
});
