import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';

import { AuthService } from '../../core/auth/auth.service';
import { API_CLIENTI, Cliente, ClientiService, Pagina, compleanno } from '../../core/clienti';
import { ClienteDialog } from './cliente-dialog';

/**
 * Elenco clienti: ricerca istantanea, ordinamento e paginazione fatti dal
 * server, inserimento e modifica in una finestra.
 */
@Component({
  selector: 'app-clienti',
  imports: [RouterLink, Button, IconField, InputIcon, InputText, Message, TableModule, ClienteDialog],
  templateUrl: './clienti.html',
  styleUrl: './clienti.scss',
})
export class Clienti {
  protected readonly auth = inject(AuthService);
  private readonly clienti = inject(ClientiService);
  protected readonly compleanno = compleanno;

  // --- stato dell'elenco (ogni cambiamento ricarica i dati dal server)
  protected readonly testo = signal('');
  private readonly ricerca = signal('');
  protected readonly primo = signal(0);
  protected readonly righe = signal(20);
  private readonly ordinaPer = signal<string | null>(null);
  private readonly ordine = signal<1 | -1>(1);

  protected readonly pagina = httpResource<Pagina<Cliente>>(() => {
    this.clienti.modifiche(); // ricarica dopo inserimenti, modifiche, eliminazioni
    const params: Record<string, string | number> = {
      page: Math.floor(this.primo() / this.righe()),
      size: this.righe(),
    };
    if (this.ricerca()) params['q'] = this.ricerca();
    const campo = this.ordinaPer();
    if (campo) params['sort'] = `${campo},${this.ordine() === 1 ? 'asc' : 'desc'}`;
    return { url: API_CLIENTI, params };
  });
  protected readonly elementi = computed(() => this.pagina.value()?.elementi ?? []);
  protected readonly totale = computed(() => this.pagina.value()?.totale ?? 0);

  // --- finestra di inserimento/modifica
  protected readonly dialogAperto = signal(false);
  protected readonly clienteScelto = signal<number | null>(null);
  /** Riga evidenziata dopo un salvataggio. */
  protected readonly evidenziato = signal<number | null>(null);

  private attesaRicerca?: ReturnType<typeof setTimeout>;

  /** Ricerca mentre si scrive, con una breve attesa per non chiamare il server a ogni tasto. */
  cerca(valore: string): void {
    this.testo.set(valore);
    clearTimeout(this.attesaRicerca);
    this.attesaRicerca = setTimeout(() => {
      this.ricerca.set(valore.trim());
      this.primo.set(0);
    }, 300);
  }

  /** Paginazione e ordinamento della tabella. */
  carica(e: TableLazyLoadEvent): void {
    this.primo.set(e.first ?? 0);
    this.righe.set(e.rows ?? 20);
    const campo = typeof e.sortField === 'string' ? e.sortField : null;
    this.ordinaPer.set(campo);
    this.ordine.set(e.sortOrder === -1 ? -1 : 1);
  }

  nuovo(): void {
    this.clienteScelto.set(null);
    this.dialogAperto.set(true);
  }

  apri(c: Cliente): void {
    this.clienteScelto.set(c.id);
    this.dialogAperto.set(true);
  }

  salvato(c: Cliente): void {
    this.evidenziato.set(c.id);
  }
}
