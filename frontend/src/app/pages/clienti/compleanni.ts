import { httpResource } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import { API_CLIENTI, Cliente, ClientiService, MESI } from '../../core/clienti';
import { ClienteDialog } from './cliente-dialog';

/** Clienti che compiono gli anni in un mese (di default quello corrente). */
@Component({
  selector: 'app-compleanni',
  imports: [FormsModule, RouterLink, Button, Message, Select, TableModule, Tag, ClienteDialog],
  templateUrl: './compleanni.html',
  styleUrl: './clienti.scss',
})
export class Compleanni {
  private readonly clienti = inject(ClientiService);

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

  protected readonly dialogAperto = signal(false);
  protected readonly clienteScelto = signal<number | null>(null);

  protected eOggi(c: Cliente): boolean {
    return c.meseNascita === this.oggi.getMonth() + 1 && c.giornoNascita === this.oggi.getDate();
  }

  protected giaPassato(c: Cliente): boolean {
    return c.meseNascita === this.oggi.getMonth() + 1 && (c.giornoNascita ?? 0) < this.oggi.getDate();
  }

  apri(c: Cliente): void {
    this.clienteScelto.set(c.id);
    this.dialogAperto.set(true);
  }
}
