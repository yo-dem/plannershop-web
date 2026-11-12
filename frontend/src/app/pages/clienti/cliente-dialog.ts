import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { Cliente, ClienteRichiesta, ClientiService, MESI } from '../../core/clienti';
import { erroriCampo, messaggioErrore } from '../../core/errori';
import { Avviso } from '../../core/ui/avviso';
import { Conferma } from '../../core/ui/conferma';
import { Notifiche } from '../../core/ui/notifiche';

/** Esito della finestra: il cliente salvato, "eliminato", oppure undefined se annullata. */
export type EsitoCliente = Cliente | 'eliminato' | undefined;

/**
 * Apre la finestra per inserire (id = null) o modificare un cliente.
 * Usata dall'elenco clienti e dalla pagina dei compleanni.
 */
export async function apriCliente(dialog: MatDialog, id: number | null): Promise<EsitoCliente> {
  const ref = dialog.open<ClienteDialog, number | null, EsitoCliente>(ClienteDialog, {
    data: id,
    width: '40rem',
    maxWidth: '100vw',
    autoFocus: 'first-tabbable',
  });
  return firstValueFrom(ref.afterClosed());
}

type Campo = 'nome' | 'cognome' | 'telefono' | 'email' | 'indirizzo' | 'note' | 'meseNascita';

@Component({
  selector: 'app-cliente-dialog',
  imports: [
    ReactiveFormsModule, MatButton, MatDialogModule, MatFormFieldModule, MatIcon, MatInput, MatProgressBar,
    MatSelectModule, Avviso,
  ],
  templateUrl: './cliente-dialog.html',
  styleUrl: './cliente-dialog.scss',
})
export class ClienteDialog implements OnInit {
  private readonly clienti = inject(ClientiService);
  private readonly ref = inject<MatDialogRef<ClienteDialog, EsitoCliente>>(MatDialogRef);
  private readonly notifiche = inject(Notifiche);
  private readonly conferma = inject(Conferma);
  protected readonly auth = inject(AuthService);
  /** Cliente da modificare; null per un nuovo cliente. */
  protected readonly id = inject<number | null>(MAT_DIALOG_DATA);

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    cognome: ['', Validators.required],
    giornoNascita: this.fb.control<number | null>(null),
    meseNascita: this.fb.control<number | null>(null),
    telefono: [''],
    email: ['', Validators.email],
    indirizzo: [''],
    note: [''],
  });

  protected readonly mesi = MESI.map((nome, i) => ({ nome: nome[0].toUpperCase() + nome.slice(1), valore: i + 1 }));
  private readonly meseScelto = toSignal(this.form.controls.meseNascita.valueChanges, { initialValue: null });
  /** Giorni del mese scelto (29 per febbraio: il compleanno non ha anno). */
  protected readonly giorni = computed(() => {
    const mese = this.meseScelto();
    const max = mese ? new Date(2024, mese, 0).getDate() : 31;
    return Array.from({ length: max }, (_, i) => i + 1);
  });

  protected readonly caricamento = signal(false);
  protected readonly invio = signal(false);
  protected readonly errore = signal<string | null>(null);
  /** Errori restituiti dal server, campo → messaggio. */
  private readonly erroriServer = signal<Record<string, string>>({});
  private version = 0;

  constructor() {
    // Un giorno che non esiste nel nuovo mese (es. 31 → febbraio) viene tolto.
    this.form.controls.meseNascita.valueChanges.subscribe(() => {
      const g = this.form.controls.giornoNascita.value;
      if (g && g > this.giorni().length) {
        this.form.controls.giornoNascita.setValue(null);
      }
    });
  }

  async ngOnInit(): Promise<void> {
    if (this.id === null) return;
    this.caricamento.set(true);
    try {
      const c = await this.clienti.dettaglio(this.id);
      this.version = c.version;
      this.form.reset({
        nome: c.nome,
        cognome: c.cognome,
        giornoNascita: c.giornoNascita,
        meseNascita: c.meseNascita,
        telefono: c.telefono ?? '',
        email: c.email ?? '',
        indirizzo: c.indirizzo ?? '',
        note: c.note ?? '',
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
    const dati: ClienteRichiesta = { ...this.form.getRawValue() };
    this.invio.set(true);
    this.errore.set(null);
    this.erroriServer.set({});
    try {
      const c =
        this.id === null
          ? await this.clienti.crea(dati)
          : await this.clienti.modifica(this.id, { ...dati, version: this.version });
      this.notifiche.successo(this.id === null ? 'Cliente inserito' : 'Cliente aggiornato');
      this.ref.close(c);
    } catch (err) {
      this.mostraErrori(err);
    } finally {
      this.invio.set(false);
    }
  }

  async elimina(): Promise<void> {
    if (this.id === null) return;
    const { nome, cognome } = this.form.getRawValue();
    const ok = await this.conferma.chiedi({
      titolo: 'Eliminare il cliente?',
      messaggio: `${nome} ${cognome} non comparirà più negli elenchi.`,
      conferma: 'Elimina',
      pericolo: true,
    });
    if (!ok) return;
    try {
      await this.clienti.elimina(this.id);
      this.notifiche.successo('Cliente eliminato');
      this.ref.close('eliminato');
    } catch (err) {
      this.notifiche.errore(messaggioErrore(err));
    }
  }

  /** Messaggio d'errore sotto il campo (validazione del modulo o del server). */
  protected messaggio(campo: Campo): string {
    const server = this.erroriServer()[campo === 'meseNascita' ? 'compleanno' : campo];
    if (server) return server;
    const c = this.form.controls[campo];
    if (c.hasError('required')) return 'Obbligatorio';
    if (c.hasError('email')) return 'Email non valida';
    return '';
  }

  /** Errori del server: quelli per campo vanno sotto i campi, gli altri in alto. */
  private mostraErrori(err: unknown): void {
    const campi = erroriCampo(err);
    this.erroriServer.set(campi);
    for (const campo of Object.keys(campi)) {
      const control = this.form.get(campo === 'compleanno' ? 'meseNascita' : campo);
      control?.setErrors({ server: true });
      control?.markAsTouched();
    }
    const conflitto = err instanceof HttpErrorResponse && err.status === 409;
    this.errore.set(
      Object.keys(campi).length
        ? 'Controlla i campi evidenziati.'
        : conflitto
          ? 'Nel frattempo un altro utente ha modificato questo cliente. Chiudi e riapri la scheda per vedere i dati aggiornati.'
          : messaggioErrore(err),
    );
  }
}
