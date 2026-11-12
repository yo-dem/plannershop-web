import { httpResource } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatPaginator } from '@angular/material/paginator';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';

import { API_CLIENTI, Cliente, ClientiService, compleanno } from '../../core/clienti';
import { StatoElenco } from '../../core/elenco';
import { Pagina } from '../../core/risorsa';
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

  protected readonly elenco = new StatoElenco();

  protected readonly dati = httpResource<Pagina<Cliente>>(() => {
    this.clienti.modifiche(); // ricarica dopo inserimenti, modifiche, eliminazioni
    return { url: API_CLIENTI, params: this.elenco.parametri() };
  });
  protected readonly elementi = computed(() => this.dati.value()?.elementi ?? []);
  protected readonly totale = computed(() => this.dati.value()?.totale ?? 0);

  async nuovo(): Promise<void> {
    this.elenco.evidenzia(await apriCliente(this.dialog, null));
  }

  async apri(c: Cliente): Promise<void> {
    this.elenco.evidenzia(await apriCliente(this.dialog, c.id));
  }
}
