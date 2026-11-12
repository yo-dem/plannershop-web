import { Injectable } from '@angular/core';

import { Prezzi, PrezziRichiesta } from './prezzi';
import { RisorsaApi } from './risorsa';

export interface Servizio extends Prezzi {
  id: number;
  nome: string;
  descrizione: string;
  note: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** Dati inviati in inserimento e modifica (in modifica si aggiunge version). */
export interface ServizioRichiesta extends PrezziRichiesta {
  nome: string;
  descrizione: string;
  note: string;
}

export const API_SERVIZI = '/api/servizi';

@Injectable({ providedIn: 'root' })
export class ServiziService extends RisorsaApi<Servizio, ServizioRichiesta> {
  constructor() {
    super(API_SERVIZI);
  }
}
