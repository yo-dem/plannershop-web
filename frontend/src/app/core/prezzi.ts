import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';

/**
 * Prezzi di prodotti e servizi (SPECIFICA §7.1). L'utente inserisce il netto
 * oppure il prezzo IVA inclusa: l'altro si calcola, arrotondando per eccesso
 * al centesimo. Il backend rifà lo stesso calcolo e fa fede: qui serve per
 * l'anteprima. I calcoli usano numeri interi (centesimi) per non avere gli
 * errori di arrotondamento dei decimali (0,1 + 0,2 ≠ 0,3).
 */

/** Quale prezzo ha inserito l'utente. */
export type CalcolaDa = 'NETTO' | 'IVATO';

export const ALIQUOTA_PREDEFINITA = 22;

/** Aliquote IVA italiane più comuni, proposte nel campo. */
export const ALIQUOTE_COMUNI = [22, 10, 5, 4, 0];

/** Campi dei prezzi inviati alle API. */
export interface PrezziRichiesta {
  aliquotaIva: number;
  calcolaDa: CalcolaDa;
  prezzoNetto: number | null;
  prezzoIvato: number | null;
  prezzoVendita: number;
}

/** Campi dei prezzi letti dalle API. */
export interface Prezzi {
  aliquotaIva: number;
  prezzoNetto: number;
  prezzoIvato: number;
  prezzoVendita: number;
}

/** Importo scritto dall'utente ("18,50", "18.50", "€ 18,50") in centesimi; null se non valido. */
export function centesimi(testo: string): number | null {
  const t = testo.replace(/[€\s]/g, '');
  if (!/^\d{1,7}([.,]\d{1,2})?$/.test(t)) return null;
  const [intero, decimali = ''] = t.split(/[.,]/);
  return Number(intero) * 100 + Number(decimali.padEnd(2, '0'));
}

/** Aliquota scritta dall'utente ("22", "22,5") in centesimi di punto (2200, 2250); null se non valida. */
export function aliquotaCentesimi(testo: string): number | null {
  const t = testo.replace(/[%\s]/g, '');
  if (!/^\d{1,3}([.,]\d{1,2})?$/.test(t)) return null;
  const [intero, decimali = ''] = t.split(/[.,]/);
  const valore = Number(intero) * 100 + Number(decimali.padEnd(2, '0'));
  return valore <= 10000 ? valore : null;
}

/** ivato = netto × (1 + aliquota/100), per eccesso al centesimo. */
export function ivatoDaNetto(netto: number, aliquota: number): number {
  return Math.ceil((netto * (10000 + aliquota)) / 10000);
}

/** netto = ivato / (1 + aliquota/100), per eccesso al centesimo. */
export function nettoDaIvato(ivato: number, aliquota: number): number {
  return Math.ceil((ivato * 10000) / (10000 + aliquota));
}

/** Centesimi → "18,50" (per i campi del modulo). */
export function testoImporto(cent: number): string {
  return (cent / 100).toFixed(2).replace('.', ',');
}

/**
 * Quale prezzo era stato inserito, per un articolo salvato: se l'ivato
 * corrisponde al netto è "NETTO", altrimenti "IVATO". Così modificare solo
 * le note non cambia di un centesimo nessuno dei due prezzi.
 */
export function fonteCoerente(p: Prezzi): CalcolaDa {
  const netto = Math.round(p.prezzoNetto * 100);
  const ivato = Math.round(p.prezzoIvato * 100);
  return ivatoDaNetto(netto, Math.round(p.aliquotaIva * 100)) === ivato ? 'NETTO' : 'IVATO';
}

// ------------------------------------------------------------- modulo

function validatore(leggi: (s: string) => number | null, codice: string) {
  return (c: AbstractControl<string>): ValidationErrors | null =>
    c.value && leggi(c.value) === null ? { [codice]: true } : null;
}

/** Importo non vuoto e non valido → errore "importo". */
export const importoValido = validatore(centesimi, 'importo');
/** Aliquota non valida (0–100, al massimo due decimali) → errore "aliquota". */
export const aliquotaValida = validatore(aliquotaCentesimi, 'aliquota');

/** Gruppo di controlli dei prezzi, da inserire nel modulo di prodotti e servizi. */
export function gruppoPrezzi(fb: FormBuilder) {
  return fb.nonNullable.group({
    aliquotaIva: [String(ALIQUOTA_PREDEFINITA), [Validators.required, aliquotaValida]],
    calcolaDa: fb.nonNullable.control<CalcolaDa>('NETTO'),
    prezzoNetto: ['', [Validators.required, importoValido]],
    prezzoIvato: ['', [Validators.required, importoValido]],
    prezzoVendita: ['', [Validators.required, importoValido]],
  });
}

export type GruppoPrezzi = ReturnType<typeof gruppoPrezzi>;

/** Valori per il modulo a partire da un articolo salvato. */
export function valoriPrezzi(p: Prezzi): GruppoPrezzi['value'] {
  return {
    aliquotaIva: String(p.aliquotaIva).replace('.', ','),
    calcolaDa: fonteCoerente(p),
    prezzoNetto: testoImporto(Math.round(p.prezzoNetto * 100)),
    prezzoIvato: testoImporto(Math.round(p.prezzoIvato * 100)),
    prezzoVendita: testoImporto(Math.round(p.prezzoVendita * 100)),
  };
}

/** Dati per le API (da un modulo valido). */
export function richiestaPrezzi(g: GruppoPrezzi): PrezziRichiesta {
  const v = g.getRawValue();
  const euro = (s: string) => (centesimi(s) ?? 0) / 100;
  return {
    aliquotaIva: (aliquotaCentesimi(v.aliquotaIva) ?? 0) / 100,
    calcolaDa: v.calcolaDa,
    prezzoNetto: v.calcolaDa === 'NETTO' ? euro(v.prezzoNetto) : null,
    prezzoIvato: v.calcolaDa === 'IVATO' ? euro(v.prezzoIvato) : null,
    prezzoVendita: euro(v.prezzoVendita),
  };
}

/**
 * Ricalcola il prezzo non inserito dall'utente. Se il prezzo inserito o
 * l'aliquota non sono validi, l'altro campo resta vuoto.
 */
export function ricalcola(g: GruppoPrezzi): void {
  const { aliquotaIva, calcolaDa, prezzoNetto, prezzoIvato } = g.getRawValue();
  const aliquota = aliquotaCentesimi(aliquotaIva);
  if (calcolaDa === 'NETTO') {
    const netto = centesimi(prezzoNetto);
    g.controls.prezzoIvato.setValue(netto !== null && aliquota !== null ? testoImporto(ivatoDaNetto(netto, aliquota)) : '');
  } else {
    const ivato = centesimi(prezzoIvato);
    g.controls.prezzoNetto.setValue(ivato !== null && aliquota !== null ? testoImporto(nettoDaIvato(ivato, aliquota)) : '');
  }
}
