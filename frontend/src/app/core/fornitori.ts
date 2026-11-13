import { Injectable } from '@angular/core';

import { RisorsaApi } from './risorsa';

export interface Fornitore {
  id: number;
  nome: string;
  indirizzo: string;
  telefono: string;
  email: string | null;
  /** Senza spazi, in maiuscolo. */
  iban: string | null;
  note: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** Dati inviati in inserimento e modifica (in modifica si aggiunge version). */
export interface FornitoreRichiesta {
  nome: string;
  indirizzo: string;
  telefono: string;
  email: string;
  iban: string;
  note: string;
}

export const API_FORNITORI = '/api/fornitori';

@Injectable({ providedIn: 'root' })
export class FornitoriService extends RisorsaApi<Fornitore, FornitoreRichiesta> {
  constructor() {
    super(API_FORNITORI);
  }
}
