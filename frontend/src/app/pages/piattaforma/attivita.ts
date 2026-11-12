import { DatePipe } from '@angular/common';
import { HttpClient, httpResource } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button, ButtonDirective, ButtonIcon, ButtonLabel } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { firstValueFrom } from 'rxjs';

import { erroriCampo, messaggioErrore } from '../../core/errori';

interface AttivitaDto {
  id: number;
  nome: string;
  attivo: boolean;
  createdAt: string;
  amministratori: string[];
}

const API = '/api/platform/tenants';

/** Gestione delle attività della piattaforma (solo SUPERADMIN). */
@Component({
  selector: 'app-attivita',
  imports: [DatePipe, ReactiveFormsModule, Button, ButtonDirective, ButtonIcon, ButtonLabel, Dialog, InputText, Message, TableModule, Tag, ToggleSwitch],
  templateUrl: './attivita.html',
  styleUrl: './attivita.scss',
})
export class Attivita {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly messaggi = inject(MessageService);
  private readonly conferma = inject(ConfirmationService);

  protected readonly elenco = httpResource<AttivitaDto[]>(() => API, { defaultValue: [] });

  // --- nuova attività
  protected readonly nuovaAperta = signal(false);
  protected readonly nuovaForm = this.fb.group({
    nome: ['', Validators.required],
    adminNome: ['', Validators.required],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  // --- modifica
  protected readonly inModifica = signal<AttivitaDto | null>(null);
  protected readonly modificaForm = this.fb.group({
    nome: ['', Validators.required],
    attivo: [true],
  });

  protected readonly invio = signal(false);
  protected readonly errore = signal<string | null>(null);

  apriNuova(): void {
    this.nuovaForm.reset();
    this.errore.set(null);
    this.nuovaAperta.set(true);
  }

  async creaAttivita(): Promise<void> {
    if (this.nuovaForm.invalid) {
      this.nuovaForm.markAllAsTouched();
      return;
    }
    await this.esegui(async () => {
      await firstValueFrom(this.http.post(API, this.nuovaForm.getRawValue()));
      this.nuovaAperta.set(false);
      this.messaggi.add({ severity: 'success', summary: 'Attività creata' });
    });
  }

  apriModifica(a: AttivitaDto): void {
    this.modificaForm.reset({ nome: a.nome, attivo: a.attivo });
    this.errore.set(null);
    this.inModifica.set(a);
  }

  async salvaModifica(): Promise<void> {
    const a = this.inModifica();
    if (!a || this.modificaForm.invalid) {
      this.modificaForm.markAllAsTouched();
      return;
    }
    await this.esegui(async () => {
      await firstValueFrom(this.http.put(`${API}/${a.id}`, this.modificaForm.getRawValue()));
      this.inModifica.set(null);
      this.messaggi.add({ severity: 'success', summary: 'Attività aggiornata' });
    });
  }

  chiediEliminazione(a: AttivitaDto): void {
    this.conferma.confirm({
      header: 'Eliminare l\'attività?',
      message: `"${a.nome}" non sarà più accessibile e i suoi utenti verranno scollegati.`,
      icon: 'pi pi-exclamation-triangle',
      acceptButtonProps: { label: 'Elimina', severity: 'danger' },
      rejectButtonProps: { label: 'Annulla', outlined: true },
      accept: async () => {
        try {
          await firstValueFrom(this.http.delete(`${API}/${a.id}`));
          this.messaggi.add({ severity: 'success', summary: 'Attività eliminata' });
          this.elenco.reload();
        } catch (err) {
          this.messaggi.add({ severity: 'error', summary: messaggioErrore(err) });
        }
      },
    });
  }

  protected nonValido(form: 'nuova' | 'modifica', campo: string): boolean {
    const gruppo: FormGroup = form === 'nuova' ? this.nuovaForm : this.modificaForm;
    const c = gruppo.get(campo);
    return !!c && c.invalid && c.touched;
  }

  private async esegui(azione: () => Promise<void>): Promise<void> {
    this.invio.set(true);
    this.errore.set(null);
    try {
      await azione();
      this.elenco.reload();
    } catch (err) {
      const campi = erroriCampo(err);
      this.errore.set(Object.values(campi)[0] ?? messaggioErrore(err));
    } finally {
      this.invio.set(false);
    }
  }
}
