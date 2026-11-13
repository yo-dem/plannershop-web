import { describe, expect, it } from 'vitest';

import { birthday } from './customers';
import { monthNames } from './i18n';

describe('birthday', () => {
  it('writes day and month in the language of the locale', () => {
    expect(birthday({ birthDay: 29, birthMonth: 2 }, monthNames('it-IT'))).toBe('29 febbraio');
    expect(birthday({ birthDay: 1, birthMonth: 12 }, monthNames('en-US'))).toBe('1 December');
  });

  it('is empty when the birthday is missing', () => {
    expect(birthday({ birthDay: null, birthMonth: null })).toBe('');
  });
});
