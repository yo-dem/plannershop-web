import { describe, expect, it } from 'vitest';

import { formatIban, isValidIban } from './iban';

describe('iban', () => {
  it('accepts valid IBANs, also with spaces and lower case', () => {
    expect(isValidIban('IT60X0542811101000000123456')).toBe(true);
    expect(isValidIban(' it60 x054 2811 1010 0000 0123 456')).toBe(true);
    expect(isValidIban('DE89370400440532013000')).toBe(true);
  });

  it('rejects wrong check digits and formats', () => {
    expect(isValidIban('IT61X0542811101000000123456')).toBe(false);
    expect(isValidIban('IT60X05428')).toBe(false);
    expect(isValidIban('')).toBe(false);
  });

  it('formats in groups of four', () => {
    expect(formatIban('IT60X0542811101000000123456')).toBe('IT60 X054 2811 1010 0000 0123 456');
    expect(formatIban(null)).toBe('');
  });
});
