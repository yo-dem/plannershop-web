import { Injectable } from '@angular/core';

import { Prezzi, PrezziRichiesta } from './prezzi';
import { RisorsaApi } from './risorsa';

export interface Prodotto extends Prezzi {
  id: number;
  /** "2026-03-15" */
  dataCarico: string | null;
  marca: string;
  descrizione: string;
  quantita: number;
  note: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** Dati inviati in inserimento e modifica (in modifica si aggiunge version). */
export interface ProdottoRichiesta extends PrezziRichiesta {
  dataCarico: string | null;
  marca: string;
  descrizione: string;
  quantita: number;
  note: string;
}

export const API_PRODOTTI = '/api/prodotti';

/**
 * Sotto questa quantità (inclusa) il prodotto è "in esaurimento".
 * Diventerà un'impostazione dell'attività (SPECIFICA §6.9, soglia_scorta_bassa).
 */
export const SOGLIA_SCORTA = 10;

export type Scorta = 'esaurito' | 'bassa' | 'ok';

export function scorta(quantita: number, soglia = SOGLIA_SCORTA): Scorta {
  if (quantita <= 0) return 'esaurito';
  return quantita <= soglia ? 'bassa' : 'ok';
}

@Injectable({ providedIn: 'root' })
export class ProdottiService extends RisorsaApi<Prodotto, ProdottoRichiesta> {
  constructor() {
    super(API_PRODOTTI);
  }
}
