import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatProgressBar } from '@angular/material/progress-bar';
import { firstValueFrom } from 'rxjs';

import { erroriCampo, messaggioErrore, messaggioSalvataggio, segnaCampi } from '../../core/errori';
import { Fornitore, FornitoriService } from '../../core/fornitori';
import { formattaIban, ibanValidator } from '../../core/iban';
import { Avviso } from '../../core/ui/avviso';
import { Conferma } from '../../core/ui/conferma';
import { Notifiche } from '../../core/ui/notifiche';

/** Esito della finestra: il fornitore salvato, "eliminato", oppure undefined se annullata. */
export type EsitoFornitore = Fornitore | 'eliminato' | undefined;

/** Apre la finestra per inserire (id = null) o modificare un fornitore. */
export async function apriFornitore(dialog: MatDialog, id: number | null): Promise<EsitoFornitore> {
  const ref = dialog.open<FornitoreDialog, number | null, EsitoFornitore>(FornitoreDialog, {
    data: id,
    width: '40rem',
    maxWidth: '100vw',
    autoFocus: 'first-tabbable',
  });
  return firstValueFrom(ref.afterClosed());
}

type Campo = 'nome' | 'indirizzo' | 'telefono' | 'email' | 'iban' | 'note';

@Component({
  selector: 'app-fornitore-dialog',
  imports: [
    ReactiveFormsModule, MatButton, MatDialogModule, MatFormFieldModule, MatIcon, MatInput, MatProgressBar, Avviso,
  ],
  templateUrl: './fornitore-dialog.html',
})
export class FornitoreDialog implements OnInit {
  private readonly fornitori = inject(FornitoriService);
  private readonly ref = inject<MatDialogRef<FornitoreDialog, EsitoFornitore>>(MatDialogRef);
  private readonly notifiche = inject(Notifiche);
  private readonly conferma = inject(Conferma);
  /** Fornitore da modificare; null per un nuovo fornitore. */
  protected readonly id = inject<number | null>(MAT_DIALOG_DATA);

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    indirizzo: ['', Validators.required],
    telefono: ['', Validators.required],
    email: ['', Validators.email],
    iban: ['', ibanValidator],
    note: [''],
  });

  protected readonly caricamento = signal(false);
  protected readonly invio = signal(false);
  protected readonly errore = signal<string | null>(null);
  /** Errori restituiti dal server, campo → messaggio. */
  private readonly erroriServer = signal<Record<string, string>>({});
  private version = 0;

  async ngOnInit(): Promise<void> {
    if (this.id === null) return;
    this.caricamento.set(true);
    try {
      const f = await this.fornitori.dettaglio(this.id);
      this.version = f.version;
      this.form.reset({
        nome: f.nome,
        indirizzo: f.indirizzo,
        telefono: f.telefono,
        email: f.email ?? '',
        iban: formattaIban(f.iban),
        note: f.note ?? '',
      });
    } catch (err) {
      this.errore.set(messaggioErrore(err));
    } finally {
      this.caricamento.set(false);
    }
  }

  /** Uscendo dal campo, l'IBAN valido viene riscritto in gruppi di quattro. */
  protected formattaIban(): void {
    const c = this.form.controls.iban;
    if (c.valid && c.value) c.setValue(formattaIban(c.value));
  }

  async salva(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const dati = this.form.getRawValue();
    this.invio.set(true);
    this.errore.set(null);
    this.erroriServer.set({});
    try {
      const f =
        this.id === null
          ? await this.fornitori.crea(dati)
          : await this.fornitori.modifica(this.id, { ...dati, version: this.version });
      this.notifiche.successo(this.id === null ? 'Fornitore inserito' : 'Fornitore aggiornato');
      this.ref.close(f);
    } catch (err) {
      const campi = erroriCampo(err);
      this.erroriServer.set(campi);
      segnaCampi(campi, (c) => this.form.get(c));
      this.errore.set(messaggioSalvataggio(err, campi));
    } finally {
      this.invio.set(false);
    }
  }

  async elimina(): Promise<void> {
    if (this.id === null) return;
    const ok = await this.conferma.chiedi({
      titolo: 'Eliminare il fornitore?',
      messaggio: `${this.form.controls.nome.value} non comparirà più negli elenchi.`,
      conferma: 'Elimina',
      pericolo: true,
    });
    if (!ok) return;
    try {
      await this.fornitori.elimina(this.id);
      this.notifiche.successo('Fornitore eliminato');
      this.ref.close('eliminato');
    } catch (err) {
      this.notifiche.errore(messaggioErrore(err));
    }
  }

  protected messaggio(campo: Campo): string {
    const server = this.erroriServer()[campo];
    if (server) return server;
    const c = this.form.controls[campo];
    if (c.hasError('required')) return 'Obbligatorio';
    if (c.hasError('email')) return 'Email non valida';
    if (c.hasError('iban')) return 'IBAN non valido: controlla le cifre';
    return '';
  }
}
