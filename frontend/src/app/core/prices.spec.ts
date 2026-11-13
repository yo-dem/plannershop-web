import { FormBuilder } from '@angular/forms';
import { describe, expect, it } from 'vitest';

import {
  cents,
  consistentBase,
  grossFromNet,
  netFromGross,
  priceGroup,
  priceRequest,
  priceValues,
  recalculate,
  vatRateHundredths,
} from './prices';

describe('prices', () => {
  it('reads amounts written with comma or dot', () => {
    expect(cents('18,50')).toBe(1850);
    expect(cents('18.5')).toBe(1850);
    expect(cents(' € 7 ')).toBe(700);
    expect(cents('0,05')).toBe(5);
    expect(cents('')).toBeNull();
    expect(cents('1.234,50')).toBeNull();
    expect(cents('12,345')).toBeNull();
    expect(cents('-3')).toBeNull();
  });

  it('reads VAT rates from 0 to 100', () => {
    expect(vatRateHundredths('22')).toBe(2200);
    expect(vatRateHundredths('22,5%')).toBe(2250);
    expect(vatRateHundredths('100')).toBe(10000);
    expect(vatRateHundredths('101')).toBeNull();
  });

  it('calculates like the backend, rounding up to the cent', () => {
    expect(grossFromNet(1000, 2200)).toBe(1220); // 10.00 → 12.20 (exact)
    expect(grossFromNet(1001, 2200)).toBe(1222); // 12.2122 → 12.22
    expect(grossFromNet(999, 400)).toBe(1039);
    expect(netFromGross(1800, 2200)).toBe(1476); // 14.754 → 14.76
    expect(netFromGross(1220, 2200)).toBe(1000);
    expect(netFromGross(5000, 0)).toBe(5000);
  });

  it('recognizes which price had been entered', () => {
    expect(consistentBase({ vatRate: 22, netPrice: 10, grossPrice: 12.2, salePrice: 15 })).toBe('NET');
    // 18.00 entered → net 14.76; but 14.76 × 1.22 = 18.01: it was the gross price.
    expect(consistentBase({ vatRate: 22, netPrice: 14.76, grossPrice: 18, salePrice: 18 })).toBe('GROSS');
  });

  it('recalculates the other field and prepares the request', () => {
    const g = priceGroup(new FormBuilder());
    g.patchValue({ netPrice: '10', salePrice: '15,5' });
    recalculate(g);
    expect(g.controls.grossPrice.value).toBe('12,20');

    g.patchValue({ priceBase: 'GROSS', grossPrice: '18', vatRate: '22' });
    recalculate(g);
    expect(g.controls.netPrice.value).toBe('14,76');
    expect(priceRequest(g)).toEqual({
      vatRate: 22,
      priceBase: 'GROSS',
      netPrice: null,
      grossPrice: 18,
      salePrice: 15.5,
    });

    g.patchValue({ vatRate: 'abc' });
    recalculate(g);
    expect(g.controls.netPrice.value).toBe('');
  });

  it('loads saved prices without changing them', () => {
    expect(priceValues({ vatRate: 22, netPrice: 14.76, grossPrice: 18, salePrice: 20 })).toEqual({
      vatRate: '22',
      priceBase: 'GROSS',
      netPrice: '14,76',
      grossPrice: '18,00',
      salePrice: '20,00',
    });
  });
});
