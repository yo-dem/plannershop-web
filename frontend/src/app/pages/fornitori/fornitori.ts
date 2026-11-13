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
import { API_FORNITORI, Fornitore, FornitoriService } from '../../core/fornitori';
import { Pagina } from '../../core/risorsa';
import { Avviso } from '../../core/ui/avviso';
import { apriFornitore } from './fornitore-dialog';

/** Elenco fornitori con ricerca su tutti i campi. */
@Component({
  selector: 'app-fornitori',
  imports: [
    MatButton, MatFormFieldModule, MatIcon, MatInput, MatPaginator, MatProgressBar, MatSort, MatSortHeader,
    MatTableModule, Avviso,
  ],
  templateUrl: './fornitori.html',
  styleUrl: './fornitori.scss',
})
export class Fornitori {
  private readonly fornitori = inject(FornitoriService);
  private readonly dialog = inject(MatDialog);
  protected readonly colonne = ['nome', 'telefono', 'email', 'indirizzo'];

  protected readonly elenco = new StatoElenco();

  protected readonly dati = httpResource<Pagina<Fornitore>>(() => {
    this.fornitori.modifiche();
    return { url: API_FORNITORI, params: this.elenco.parametri() };
  });
  protected readonly elementi = computed(() => this.dati.value()?.elementi ?? []);
  protected readonly totale = computed(() => this.dati.value()?.totale ?? 0);

  async nuovo(): Promise<void> {
    this.elenco.evidenzia(await apriFornitore(this.dialog, null));
  }

  async apri(f: Fornitore): Promise<void> {
    this.elenco.evidenzia(await apriFornitore(this.dialog, f.id));
  }
}
