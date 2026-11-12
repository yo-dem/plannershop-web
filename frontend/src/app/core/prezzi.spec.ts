import { FormBuilder } from '@angular/forms';
import { describe, expect, it } from 'vitest';

import {
  aliquotaCentesimi,
  centesimi,
  fonteCoerente,
  gruppoPrezzi,
  ivatoDaNetto,
  nettoDaIvato,
  ricalcola,
  richiestaPrezzi,
  valoriPrezzi,
} from './prezzi';

describe('prezzi', () => {
  it('legge gli importi scritti in italiano', () => {
    expect(centesimi('18,50')).toBe(1850);
    expect(centesimi('18.5')).toBe(1850);
    expect(centesimi(' € 7 ')).toBe(700);
    expect(centesimi('0,05')).toBe(5);
    expect(centesimi('')).toBeNull();
    expect(centesimi('1.234,50')).toBeNull();
    expect(centesimi('12,345')).toBeNull();
    expect(centesimi('-3')).toBeNull();
  });

  it('legge le aliquote da 0 a 100', () => {
    expect(aliquotaCentesimi('22')).toBe(2200);
    expect(aliquotaCentesimi('22,5%')).toBe(2250);
    expect(aliquotaCentesimi('100')).toBe(10000);
    expect(aliquotaCentesimi('101')).toBeNull();
  });

  it('calcola come il backend, per eccesso al centesimo', () => {
    expect(ivatoDaNetto(1000, 2200)).toBe(1220); // 10,00 → 12,20 (esatto)
    expect(ivatoDaNetto(1001, 2200)).toBe(1222); // 12,2122 → 12,22
    expect(ivatoDaNetto(999, 400)).toBe(1039);
    expect(nettoDaIvato(1800, 2200)).toBe(1476); // 14,754 → 14,76
    expect(nettoDaIvato(1220, 2200)).toBe(1000);
    expect(nettoDaIvato(5000, 0)).toBe(5000);
  });

  it('riconosce quale prezzo era stato inserito', () => {
    expect(fonteCoerente({ aliquotaIva: 22, prezzoNetto: 10, prezzoIvato: 12.2, prezzoVendita: 15 })).toBe('NETTO');
    // 18,00 inserito → netto 14,76; ma 14,76 × 1,22 = 18,01: era l'ivato.
    expect(fonteCoerente({ aliquotaIva: 22, prezzoNetto: 14.76, prezzoIvato: 18, prezzoVendita: 18 })).toBe('IVATO');
  });

  it('ricalcola il campo non inserito e prepara la richiesta', () => {
    const g = gruppoPrezzi(new FormBuilder());
    g.patchValue({ prezzoNetto: '10', prezzoVendita: '15,5' });
    ricalcola(g);
    expect(g.controls.prezzoIvato.value).toBe('12,20');

    g.patchValue({ calcolaDa: 'IVATO', prezzoIvato: '18', aliquotaIva: '22' });
    ricalcola(g);
    expect(g.controls.prezzoNetto.value).toBe('14,76');
    expect(richiestaPrezzi(g)).toEqual({
      aliquotaIva: 22,
      calcolaDa: 'IVATO',
      prezzoNetto: null,
      prezzoIvato: 18,
      prezzoVendita: 15.5,
    });

    g.patchValue({ aliquotaIva: 'abc' });
    ricalcola(g);
    expect(g.controls.prezzoNetto.value).toBe('');
  });

  it('carica i prezzi salvati senza cambiarli', () => {
    expect(valoriPrezzi({ aliquotaIva: 22, prezzoNetto: 14.76, prezzoIvato: 18, prezzoVendita: 20 })).toEqual({
      aliquotaIva: '22',
      calcolaDa: 'IVATO',
      prezzoNetto: '14,76',
      prezzoIvato: '18,00',
      prezzoVendita: '20,00',
    });
  });
});
