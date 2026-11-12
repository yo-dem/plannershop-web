import { HttpClient } from '@angular/common/http';
import { inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

/** Risposta paginata delle API (vedi Pagina nel backend). */
export interface Pagina<T> {
  elementi: T[];
  totale: number;
  pagina: number;
  dimensione: number;
}

/**
 * Chiamate CRUD a una risorsa delle API (clienti, prodotti, servizi...).
 * Le sottoclassi indicano solo l'indirizzo, es. `super('/api/prodotti')`.
 */
export abstract class RisorsaApi<T, R> {
  private readonly http = inject(HttpClient);

  /**
   * Cresce a ogni inserimento, modifica o eliminazione: chi mostra questi
   * dati (elenchi, avvisi) lo legge per ricaricarsi.
   */
  readonly modifiche = signal(0);

  protected constructor(readonly url: string) {}

  dettaglio(id: number): Promise<T> {
    return firstValueFrom(this.http.get<T>(`${this.url}/${id}`));
  }

  async crea(dati: R): Promise<T> {
    const r = await firstValueFrom(this.http.post<T>(this.url, dati));
    this.modificato();
    return r;
  }

  async modifica(id: number, dati: R & { version: number }): Promise<T> {
    const r = await firstValueFrom(this.http.put<T>(`${this.url}/${id}`, dati));
    this.modificato();
    return r;
  }

  async elimina(id: number): Promise<void> {
    await firstValueFrom(this.http.delete(`${this.url}/${id}`));
    this.modificato();
  }

  private modificato(): void {
    this.modifiche.update((n) => n + 1);
  }
}
