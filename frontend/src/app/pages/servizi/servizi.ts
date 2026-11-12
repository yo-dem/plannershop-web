import { CurrencyPipe } from '@angular/common';
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

import { StatoElenco } from '../../core/elenco';
import { Pagina } from '../../core/risorsa';
import { API_SERVIZI, Servizio, ServiziService } from '../../core/servizi';
import { Avviso } from '../../core/ui/avviso';
import { apriServizio } from './servizio-dialog';

/** Elenco dei servizi offerti (trattamenti, prestazioni). */
@Component({
  selector: 'app-servizi',
  imports: [
    CurrencyPipe, MatButton, MatFormFieldModule, MatIcon, MatInput, MatPaginator, MatProgressBar, MatSort,
    MatSortHeader, MatTableModule, Avviso,
  ],
  templateUrl: './servizi.html',
  styleUrl: '../prodotti/prodotti.scss',
})
export class Servizi {
  private readonly servizi = inject(ServiziService);
  private readonly dialog = inject(MatDialog);
  protected readonly colonne = ['nome', 'descrizione', 'prezzoVendita'];

  protected readonly elenco = new StatoElenco();

  protected readonly dati = httpResource<Pagina<Servizio>>(() => {
    this.servizi.modifiche();
    return { url: API_SERVIZI, params: this.elenco.parametri() };
  });
  protected readonly elementi = computed(() => this.dati.value()?.elementi ?? []);
  protected readonly totale = computed(() => this.dati.value()?.totale ?? 0);

  async nuovo(): Promise<void> {
    this.elenco.evidenzia(await apriServizio(this.dialog, null));
  }

  async apri(s: Servizio): Promise<void> {
    this.elenco.evidenzia(await apriServizio(this.dialog, s.id));
  }
}
