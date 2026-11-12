import { httpResource } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';

import { API_CLIENTI, Cliente, ClientiService, MESI } from '../../core/clienti';
import { Avviso } from '../../core/ui/avviso';
import { apriCliente } from './cliente-dialog';

/** Clienti che compiono gli anni in un mese (di default quello corrente). */
@Component({
  selector: 'app-compleanni',
  imports: [
    RouterLink, MatIconButton, MatFormFieldModule, MatIcon, MatProgressBar, MatSelectModule, MatTableModule, Avviso,
  ],
  templateUrl: './compleanni.html',
  styleUrl: './clienti.scss',
})
export class Compleanni {
  private readonly clienti = inject(ClientiService);
  private readonly dialog = inject(MatDialog);
  protected readonly colonne = ['giorno', 'cliente', 'telefono', 'email'];

  protected readonly mesi = MESI.map((nome, i) => ({ nome: nome[0].toUpperCase() + nome.slice(1), valore: i + 1 }));
  private readonly oggi = new Date();
  protected readonly mese = signal(this.oggi.getMonth() + 1);

  protected readonly elenco = httpResource<Cliente[]>(
    () => {
      this.clienti.modifiche();
      return { url: `${API_CLIENTI}/compleanni`, params: { mese: this.mese() } };
    },
    { defaultValue: [] },
  );

  protected eOggi(c: Cliente): boolean {
    return c.meseNascita === this.oggi.getMonth() + 1 && c.giornoNascita === this.oggi.getDate();
  }

  protected giaPassato(c: Cliente): boolean {
    return c.meseNascita === this.oggi.getMonth() + 1 && (c.giornoNascita ?? 0) < this.oggi.getDate();
  }

  apri(c: Cliente): void {
    void apriCliente(this.dialog, c.id);
  }
}
