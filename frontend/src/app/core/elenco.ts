import { signal } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';

/**
 * Stato di un elenco con ricerca, ordinamento e paginazione fatti dal server.
 * La pagina lo usa per costruire i parametri della richiesta:
 * `httpResource(() => ({ url, params: elenco.parametri() }))`.
 */
export class StatoElenco {
  /** Testo nel campo di ricerca (aggiornato a ogni tasto). */
  readonly testo = signal('');
  /** Testo effettivamente cercato (dopo una breve attesa). */
  private readonly ricerca = signal('');
  readonly pagina = signal(0);
  readonly righe = signal(20);
  private readonly ordinamento = signal<Sort>({ active: '', direction: '' });
  /** Riga evidenziata dopo un salvataggio. */
  readonly evidenziato = signal<number | null>(null);

  private attesa?: ReturnType<typeof setTimeout>;

  /** Ricerca mentre si scrive, senza chiamare il server a ogni tasto. */
  cerca(valore: string): void {
    this.testo.set(valore);
    clearTimeout(this.attesa);
    this.attesa = setTimeout(() => {
      this.ricerca.set(valore.trim());
      this.pagina.set(0);
    }, 300);
  }

  cambiaPagina(e: PageEvent): void {
    this.pagina.set(e.pageIndex);
    this.righe.set(e.pageSize);
  }

  ordina(s: Sort): void {
    this.ordinamento.set(s);
    this.pagina.set(0);
  }

  /** Torna alla prima pagina (es. quando cambia un filtro). */
  primaPagina(): void {
    this.pagina.set(0);
  }

  /** Evidenzia l'elemento appena salvato (non quello eliminato). */
  evidenzia(esito: { id: number } | 'eliminato' | undefined): void {
    if (esito && esito !== 'eliminato') this.evidenziato.set(esito.id);
  }

  /** Parametri page, size, q, sort per le API. */
  parametri(): Record<string, string | number> {
    const params: Record<string, string | number> = { page: this.pagina(), size: this.righe() };
    if (this.ricerca()) params['q'] = this.ricerca();
    const { active, direction } = this.ordinamento();
    if (active && direction) params['sort'] = `${active},${direction}`;
    return params;
  }
}
