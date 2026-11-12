import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

/** Risposta paginata delle API (vedi Pagina nel backend). */
export interface Pagina<T> {
  elementi: T[];
  totale: number;
  pagina: number;
  dimensione: number;
}

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

/** Dati inviati in inserimento e modifica (version solo in modifica). */
export interface ClienteRichiesta {
  nome: string;
  cognome: string;
  giornoNascita: number | null;
  meseNascita: number | null;
  telefono: string;
  email: string;
  indirizzo: string;
  note: string;
  version?: number;
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
export class ClientiService {
  private readonly http = inject(HttpClient);

  /**
   * Cresce a ogni inserimento, modifica o eliminazione: chi mostra dati dei
   * clienti (elenco, avviso compleanni) lo legge per ricaricarsi.
   */
  readonly modifiche = signal(0);

  dettaglio(id: number): Promise<Cliente> {
    return firstValueFrom(this.http.get<Cliente>(`${API_CLIENTI}/${id}`));
  }

  async crea(dati: ClienteRichiesta): Promise<Cliente> {
    const c = await firstValueFrom(this.http.post<Cliente>(API_CLIENTI, dati));
    this.modifiche.update((n) => n + 1);
    return c;
  }

  async modifica(id: number, dati: ClienteRichiesta): Promise<Cliente> {
    const c = await firstValueFrom(this.http.put<Cliente>(`${API_CLIENTI}/${id}`, dati));
    this.modifiche.update((n) => n + 1);
    return c;
  }

  async elimina(id: number): Promise<void> {
    await firstValueFrom(this.http.delete(`${API_CLIENTI}/${id}`));
    this.modifiche.update((n) => n + 1);
  }
}
