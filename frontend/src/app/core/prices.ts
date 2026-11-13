import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';

import { LOCALE } from './i18n';

/**
 * Prices of products and services (SPECIFICA §7.1). The user enters the net
 * or the gross price (VAT included): the other one is calculated, rounded up to
 * the cent. The backend repeats the same calculation and is authoritative: here
 * it is for the preview. Calculations use integers (cents) to avoid decimal
 * rounding errors (0.1 + 0.2 ≠ 0.3).
 */

/** Which price the user entered. */
export type PriceBase = 'NET' | 'GROSS';

export const DEFAULT_VAT_RATE = 22;

/** Most common Italian VAT rates, suggested in the field. */
export const COMMON_VAT_RATES = [22, 10, 5, 4, 0];

/** Price fields sent to the APIs. */
export interface PriceRequest {
  vatRate: number;
  priceBase: PriceBase;
  netPrice: number | null;
  grossPrice: number | null;
  salePrice: number;
}

/** Price fields read from the APIs. */
export interface Prices {
  vatRate: number;
  netPrice: number;
  grossPrice: number;
  salePrice: number;
}

/** Amount typed by the user ("18,50", "18.50", "€ 18,50") in cents; null if not valid. */
export function cents(text: string): number | null {
  const t = text.replace(/[€\s]/g, '');
  if (!/^\d{1,7}([.,]\d{1,2})?$/.test(t)) return null;
  const [integer, decimals = ''] = t.split(/[.,]/);
  return Number(integer) * 100 + Number(decimals.padEnd(2, '0'));
}

/** VAT rate typed by the user ("22", "22,5") in hundredths of a point (2200, 2250); null if not valid. */
export function vatRateHundredths(text: string): number | null {
  const t = text.replace(/[%\s]/g, '');
  if (!/^\d{1,3}([.,]\d{1,2})?$/.test(t)) return null;
  const [integer, decimals = ''] = t.split(/[.,]/);
  const value = Number(integer) * 100 + Number(decimals.padEnd(2, '0'));
  return value <= 10000 ? value : null;
}

/** gross = net × (1 + rate/100), rounded up to the cent. */
export function grossFromNet(net: number, vatRate: number): number {
  return Math.ceil((net * (10000 + vatRate)) / 10000);
}

/** net = gross / (1 + rate/100), rounded up to the cent. */
export function netFromGross(gross: number, vatRate: number): number {
  return Math.ceil((gross * 10000) / (10000 + vatRate));
}

/** Decimal separator of the UI locale ("," in Italian). */
const DECIMAL_SEPARATOR = new Intl.NumberFormat(LOCALE).format(1.5).charAt(1);

/** Cents → "18,50" (for the form fields, with the decimal separator of the locale). */
export function amountText(c: number): string {
  return (c / 100).toFixed(2).replace('.', DECIMAL_SEPARATOR);
}

/**
 * Which price had been entered, for a saved item: if the gross price matches
 * the net one it is "NET", otherwise "GROSS". So saving after changing only the
 * notes does not change either price by a cent.
 */
export function consistentBase(p: Prices): PriceBase {
  const net = Math.round(p.netPrice * 100);
  const gross = Math.round(p.grossPrice * 100);
  return grossFromNet(net, Math.round(p.vatRate * 100)) === gross ? 'NET' : 'GROSS';
}

// ------------------------------------------------------------- form

function validator(parse: (s: string) => number | null, code: string) {
  return (c: AbstractControl<string>): ValidationErrors | null =>
    c.value && parse(c.value) === null ? { [code]: true } : null;
}

/** Non-empty, invalid amount → "amount" error. */
export const amountValidator = validator(cents, 'amount');
/** Invalid VAT rate (0–100, at most two decimals) → "vatRate" error. */
export const vatRateValidator = validator(vatRateHundredths, 'vatRate');

/** Group of price controls, to put in the product and service forms. */
export function priceGroup(fb: FormBuilder) {
  return fb.nonNullable.group({
    vatRate: [String(DEFAULT_VAT_RATE), [Validators.required, vatRateValidator]],
    priceBase: fb.nonNullable.control<PriceBase>('NET'),
    netPrice: ['', [Validators.required, amountValidator]],
    grossPrice: ['', [Validators.required, amountValidator]],
    salePrice: ['', [Validators.required, amountValidator]],
  });
}

export type PriceGroup = ReturnType<typeof priceGroup>;

/** Form values from a saved item. */
export function priceValues(p: Prices): PriceGroup['value'] {
  return {
    vatRate: String(p.vatRate).replace('.', DECIMAL_SEPARATOR),
    priceBase: consistentBase(p),
    netPrice: amountText(Math.round(p.netPrice * 100)),
    grossPrice: amountText(Math.round(p.grossPrice * 100)),
    salePrice: amountText(Math.round(p.salePrice * 100)),
  };
}

/** Data for the APIs (from a valid form). */
export function priceRequest(g: PriceGroup): PriceRequest {
  const v = g.getRawValue();
  const euro = (s: string) => (cents(s) ?? 0) / 100;
  return {
    vatRate: (vatRateHundredths(v.vatRate) ?? 0) / 100,
    priceBase: v.priceBase,
    netPrice: v.priceBase === 'NET' ? euro(v.netPrice) : null,
    grossPrice: v.priceBase === 'GROSS' ? euro(v.grossPrice) : null,
    salePrice: euro(v.salePrice),
  };
}

/**
 * Recalculates the price the user did not enter. If the entered price or the
 * VAT rate are not valid, the other field stays empty.
 */
export function recalculate(g: PriceGroup): void {
  const { vatRate, priceBase, netPrice, grossPrice } = g.getRawValue();
  const rate = vatRateHundredths(vatRate);
  if (priceBase === 'NET') {
    const net = cents(netPrice);
    g.controls.grossPrice.setValue(net !== null && rate !== null ? amountText(grossFromNet(net, rate)) : '');
  } else {
    const gross = cents(grossPrice);
    g.controls.netPrice.setValue(gross !== null && rate !== null ? amountText(netFromGross(gross, rate)) : '');
  }
}
