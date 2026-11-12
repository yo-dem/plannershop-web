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
import { gruppoPrezzi, richiestaPrezzi, valoriPrezzi } from '../../core/prezzi';
import { Servizio, ServiziService } from '../../core/servizi';
import { Avviso } from '../../core/ui/avviso';
import { CampiPrezzo } from '../../core/ui/campi-prezzo';
import { Conferma } from '../../core/ui/conferma';
import { Notifiche } from '../../core/ui/notifiche';

/** Esito della finestra: il servizio salvato, "eliminato", oppure undefined se annullata. */
export type EsitoServizio = Servizio | 'eliminato' | undefined;

/** Apre la finestra per inserire (id = null) o modificare un servizio. */
export async function apriServizio(dialog: MatDialog, id: number | null): Promise<EsitoServizio> {
  const ref = dialog.open<ServizioDialog, number | null, EsitoServizio>(ServizioDialog, {
    data: id,
    width: '40rem',
    maxWidth: '100vw',
    autoFocus: 'first-tabbable',
  });
  return firstValueFrom(ref.afterClosed());
}

type Campo = 'nome' | 'descrizione' | 'note';

@Component({
  selector: 'app-servizio-dialog',
  imports: [
    ReactiveFormsModule, MatButton, MatDialogModule, MatFormFieldModule, MatIcon, MatInput, MatProgressBar, Avviso,
    CampiPrezzo,
  ],
  templateUrl: './servizio-dialog.html',
})
export class ServizioDialog implements OnInit {
  private readonly servizi = inject(ServiziService);
  private readonly ref = inject<MatDialogRef<ServizioDialog, EsitoServizio>>(MatDialogRef);
  private readonly notifiche = inject(Notifiche);
  private readonly conferma = inject(Conferma);
  /** Servizio da modificare; null per un nuovo servizio. */
  protected readonly id = inject<number | null>(MAT_DIALOG_DATA);

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    descrizione: ['', Validators.required],
    prezzi: gruppoPrezzi(this.fb),
    note: [''],
  });

  protected readonly caricamento = signal(false);
  protected readonly invio = signal(false);
  protected readonly errore = signal<string | null>(null);
  /** Errori restituiti dal server, campo → messaggio. */
  protected readonly erroriServer = signal<Record<string, string>>({});
  private version = 0;

  async ngOnInit(): Promise<void> {
    if (this.id === null) return;
    this.caricamento.set(true);
    try {
      const s = await this.servizi.dettaglio(this.id);
      this.version = s.version;
      this.form.reset({
        nome: s.nome,
        descrizione: s.descrizione,
        prezzi: valoriPrezzi(s),
        note: s.note ?? '',
      });
    } catch (err) {
      this.errore.set(messaggioErrore(err));
    } finally {
      this.caricamento.set(false);
    }
  }

  async salva(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const dati = {
      nome: v.nome,
      descrizione: v.descrizione,
      note: v.note,
      ...richiestaPrezzi(this.form.controls.prezzi),
    };
    this.invio.set(true);
    this.errore.set(null);
    this.erroriServer.set({});
    try {
      const s =
        this.id === null
          ? await this.servizi.crea(dati)
          : await this.servizi.modifica(this.id, { ...dati, version: this.version });
      this.notifiche.successo(this.id === null ? 'Servizio inserito' : 'Servizio aggiornato');
      this.ref.close(s);
    } catch (err) {
      const campi = erroriCampo(err);
      this.erroriServer.set(campi);
      segnaCampi(campi, (c) => this.form.get(c) ?? this.form.controls.prezzi.get(this.campoPrezzo(c)));
      this.errore.set(messaggioSalvataggio(err, campi));
    } finally {
      this.invio.set(false);
    }
  }

  async elimina(): Promise<void> {
    if (this.id === null) return;
    const { nome } = this.form.getRawValue();
    const ok = await this.conferma.chiedi({
      titolo: 'Eliminare il servizio?',
      messaggio: `${nome} non comparirà più negli elenchi. Le vendite passate restano invariate.`,
      conferma: 'Elimina',
      pericolo: true,
    });
    if (!ok) return;
    try {
      await this.servizi.elimina(this.id);
      this.notifiche.successo('Servizio eliminato');
      this.ref.close('eliminato');
    } catch (err) {
      this.notifiche.errore(messaggioErrore(err));
    }
  }

  /** L'errore "prezzo" del server riguarda il prezzo inserito dall'utente. */
  private campoPrezzo(campo: string): string {
    if (campo !== 'prezzo') return campo;
    return this.form.controls.prezzi.controls.calcolaDa.value === 'NETTO' ? 'prezzoNetto' : 'prezzoIvato';
  }

  protected messaggio(campo: Campo): string {
    const server = this.erroriServer()[campo];
    if (server) return server;
    const c = this.form.controls[campo];
    if (c.hasError('required')) return 'Obbligatorio';
    return '';
  }
}
