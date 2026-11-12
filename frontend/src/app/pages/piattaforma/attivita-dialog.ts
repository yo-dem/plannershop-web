import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { firstValueFrom } from 'rxjs';

import { erroriCampo, messaggioErrore } from '../../core/errori';
import { Avviso } from '../../core/ui/avviso';

export interface AttivitaDto {
  id: number;
  nome: string;
  attivo: boolean;
  createdAt: string;
  amministratori: string[];
}

export const API_ATTIVITA = '/api/platform/tenants';

/**
 * Finestra per creare un'attività con il suo primo amministratore (dati = null)
 * o per modificarne nome e stato. Si chiude con true dopo un salvataggio.
 */
@Component({
  selector: 'app-attivita-dialog',
  imports: [ReactiveFormsModule, MatButton, MatDialogModule, MatFormFieldModule, MatIcon, MatInput, MatSlideToggle, Avviso],
  template: `
    <h2 mat-dialog-title>{{ attivita ? 'Modifica attività' : 'Nuova attività' }}</h2>
    <form [formGroup]="form" (ngSubmit)="salva()" novalidate>
      <mat-dialog-content>
        @if (errore(); as e) {
          <app-avviso tipo="errore">{{ e }}</app-avviso>
        }
        <div class="modulo">
          <mat-form-field>
            <mat-label>Nome attività</mat-label>
            <input matInput id="nome" formControlName="nome" required />
            <mat-error>Obbligatorio</mat-error>
          </mat-form-field>

          @if (attivita) {
            <mat-slide-toggle formControlName="attivo">Attiva (se spenta, nessuno può accedere)</mat-slide-toggle>
          } @else {
            <h3>Primo amministratore</h3>
            <mat-form-field>
              <mat-label>Nome e cognome</mat-label>
              <input matInput id="adminNome" formControlName="adminNome" required />
              <mat-error>Obbligatorio</mat-error>
            </mat-form-field>
            <mat-form-field>
              <mat-label>Email</mat-label>
              <input matInput id="adminEmail" type="email" formControlName="adminEmail" required />
              <mat-error>Email non valida</mat-error>
            </mat-form-field>
            <mat-form-field>
              <mat-label>Password provvisoria</mat-label>
              <input matInput id="adminPassword" formControlName="adminPassword" autocomplete="off" required />
              <mat-hint>Comunicala all'amministratore: dovrà cambiarla al primo accesso.</mat-hint>
              <mat-error>Almeno 8 caratteri</mat-error>
            </mat-form-field>
          }
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button matButton type="button" mat-dialog-close>Annulla</button>
        <button matButton="filled" type="submit" [disabled]="invio()">
          <mat-icon>check</mat-icon> {{ attivita ? 'Salva' : 'Crea' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    h3 {
      margin: 0.5rem 0 0.25rem;
      font: var(--mat-sys-title-small);
    }
    mat-slide-toggle {
      margin: 0.5rem 0;
    }
  `,
})
export class AttivitaDialog {
  private readonly http = inject(HttpClient);
  private readonly ref = inject<MatDialogRef<AttivitaDialog, boolean>>(MatDialogRef);
  /** Attività da modificare; null per crearne una nuova. */
  protected readonly attivita = inject<AttivitaDto | null>(MAT_DIALOG_DATA);

  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly form = this.attivita
    ? this.fb.group({ nome: [this.attivita.nome, Validators.required], attivo: [this.attivita.attivo] })
    : this.fb.group({
        nome: ['', Validators.required],
        adminNome: ['', Validators.required],
        adminEmail: ['', [Validators.required, Validators.email]],
        adminPassword: ['', [Validators.required, Validators.minLength(8)]],
      });

  protected readonly invio = signal(false);
  protected readonly errore = signal<string | null>(null);

  async salva(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.invio.set(true);
    this.errore.set(null);
    try {
      const dati = this.form.getRawValue();
      await firstValueFrom(
        this.attivita ? this.http.put(`${API_ATTIVITA}/${this.attivita.id}`, dati) : this.http.post(API_ATTIVITA, dati),
      );
      this.ref.close(true);
    } catch (err) {
      this.errore.set(Object.values(erroriCampo(err))[0] ?? messaggioErrore(err));
    } finally {
      this.invio.set(false);
    }
  }
}
