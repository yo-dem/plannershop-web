import { Injectable } from '@angular/core';

import { RisorsaApi } from './risorsa';

export interface Cliente {
  id: number;
  nome: string;
  cognome: string;
  giornoNascita: number | null;
  meseNascita: number | null;
  telefono: string | null;
  email: string | null;
  indirizzo: string | null;
  note: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** Dati inviati in inserimento e modifica (in modifica si aggiunge version). */
export interface ClienteRichiesta {
  nome: string;
  cognome: string;
  giornoNascita: number | null;
  meseNascita: number | null;
  telefono: string;
  email: string;
  indirizzo: string;
  note: string;
}

export const API_CLIENTI = '/api/clienti';

export const MESI = [
  'gennaio', 'febbraio', 'marzo', 'aprile', 'maggio', 'giugno',
  'luglio', 'agosto', 'settembre', 'ottobre', 'novembre', 'dicembre',
];

/** "29 febbraio", oppure stringa vuota se il compleanno non è indicato. */
export function compleanno(c: Pick<Cliente, 'giornoNascita' | 'meseNascita'>): string {
  return c.giornoNascita && c.meseNascita ? `${c.giornoNascita} ${MESI[c.meseNascita - 1]}` : '';
}

/** Chiamate alle API dei clienti, condivise tra le pagine. */
@Injectable({ providedIn: 'root' })
export class ClientiService extends RisorsaApi<Cliente, ClienteRichiesta> {
  constructor() {
    super(API_CLIENTI);
  }
}
