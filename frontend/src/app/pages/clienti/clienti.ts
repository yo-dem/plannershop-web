import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatSort, MatSortHeader, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';

import { API_CLIENTI, Cliente, ClientiService, Pagina, compleanno } from '../../core/clienti';
import { Avviso } from '../../core/ui/avviso';
import { apriCliente } from './cliente-dialog';

/**
 * Elenco clienti: ricerca istantanea, ordinamento e paginazione fatti dal
 * server, inserimento e modifica in una finestra.
 */
@Component({
  selector: 'app-clienti',
  imports: [
    RouterLink, MatButton, MatFormFieldModule, MatIcon, MatInput, MatPaginator, MatProgressBar, MatSort,
    MatSortHeader, MatTableModule, Avviso,
  ],
  templateUrl: './clienti.html',
  styleUrl: './clienti.scss',
})
export class Clienti {
  private readonly clienti = inject(ClientiService);
  private readonly dialog = inject(MatDialog);
  protected readonly compleanno = compleanno;
  protected readonly colonne = ['cognome', 'telefono', 'email', 'compleanno'];

  // --- stato dell'elenco (ogni cambiamento ricarica i dati dal server)
  protected readonly testo = signal('');
  private readonly ricerca = signal('');
  protected readonly pagina = signal(0);
  protected readonly righe = signal(20);
  private readonly ordinamento = signal<Sort>({ active: '', direction: '' });

  protected readonly dati = httpResource<Pagina<Cliente>>(() => {
    this.clienti.modifiche(); // ricarica dopo inserimenti, modifiche, eliminazioni
    const params: Record<string, string | number> = { page: this.pagina(), size: this.righe() };
    if (this.ricerca()) params['q'] = this.ricerca();
    const { active, direction } = this.ordinamento();
    if (active && direction) params['sort'] = `${active},${direction}`;
    return { url: API_CLIENTI, params };
  });
  protected readonly elementi = computed(() => this.dati.value()?.elementi ?? []);
  protected readonly totale = computed(() => this.dati.value()?.totale ?? 0);

  /** Riga evidenziata dopo un salvataggio. */
  protected readonly evidenziato = signal<number | null>(null);

  private attesaRicerca?: ReturnType<typeof setTimeout>;

  /** Ricerca mentre si scrive, con una breve attesa per non chiamare il server a ogni tasto. */
  cerca(valore: string): void {
    this.testo.set(valore);
    clearTimeout(this.attesaRicerca);
    this.attesaRicerca = setTimeout(() => {
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

  async nuovo(): Promise<void> {
    this.evidenzia(await apriCliente(this.dialog, null));
  }

  async apri(c: Cliente): Promise<void> {
    this.evidenzia(await apriCliente(this.dialog, c.id));
  }

  private evidenzia(esito: Cliente | 'eliminato' | undefined): void {
    if (esito && esito !== 'eliminato') this.evidenziato.set(esito.id);
  }
}
