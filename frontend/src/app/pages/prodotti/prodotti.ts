import { CurrencyPipe, DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatPaginator } from '@angular/material/paginator';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';

import { StatoElenco } from '../../core/elenco';
import { API_PRODOTTI, Prodotto, ProdottiService, scorta } from '../../core/prodotti';
import { Pagina } from '../../core/risorsa';
import { Avviso } from '../../core/ui/avviso';
import { apriProdotto } from './prodotto-dialog';

/** Elenco prodotti con giacenza evidenziata: esaurito in rosso, in esaurimento in arancione. */
@Component({
  selector: 'app-prodotti',
  imports: [
    CurrencyPipe, DatePipe, MatButton, MatFormFieldModule, MatIcon, MatInput, MatPaginator, MatProgressBar,
    MatSlideToggle, MatSort, MatSortHeader, MatTableModule, Avviso,
  ],
  templateUrl: './prodotti.html',
  styleUrl: './prodotti.scss',
})
export class Prodotti {
  private readonly prodotti = inject(ProdottiService);
  private readonly dialog = inject(MatDialog);
  protected readonly scorta = scorta;
  protected readonly colonne = ['marca', 'quantita', 'prezzoVendita', 'dataCarico'];

  protected readonly elenco = new StatoElenco();
  /** Solo prodotti con quantità maggiore di zero. */
  protected readonly soloDisponibili = signal(false);

  protected readonly dati = httpResource<Pagina<Prodotto>>(() => {
    this.prodotti.modifiche();
    const params = this.elenco.parametri();
    if (this.soloDisponibili()) params['disponibili'] = 'true';
    return { url: API_PRODOTTI, params };
  });
  protected readonly elementi = computed(() => this.dati.value()?.elementi ?? []);
  protected readonly totale = computed(() => this.dati.value()?.totale ?? 0);

  filtraDisponibili(attivo: boolean): void {
    this.soloDisponibili.set(attivo);
    this.elenco.primaPagina();
  }

  async nuovo(): Promise<void> {
    this.elenco.evidenzia(await apriProdotto(this.dialog, null));
  }

  async apri(p: Prodotto): Promise<void> {
    this.elenco.evidenzia(await apriProdotto(this.dialog, p.id));
  }
}
