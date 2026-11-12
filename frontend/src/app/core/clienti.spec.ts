import { describe, expect, it } from 'vitest';

import { compleanno } from './clienti';

describe('compleanno', () => {
  it('scrive giorno e mese in italiano', () => {
    expect(compleanno({ giornoNascita: 29, meseNascita: 2 })).toBe('29 febbraio');
    expect(compleanno({ giornoNascita: 1, meseNascita: 12 })).toBe('1 dicembre');
  });

  it('è vuoto se il compleanno non è indicato', () => {
    expect(compleanno({ giornoNascita: null, meseNascita: null })).toBe('');
  });
});
