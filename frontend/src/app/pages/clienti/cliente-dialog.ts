import { Component, computed, effect, inject, input, model, output, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button, ButtonDirective, ButtonIcon, ButtonLabel } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';

import { AuthService } from '../../core/auth/auth.service';
import { Cliente, ClienteRichiesta, ClientiService, MESI } from '../../core/clienti';
import { erroriCampo, messaggioErrore } from '../../core/errori';

/**
 * Finestra per inserire (clienteId = null) o modificare un cliente.
 * Usata dall'elenco clienti e dalla pagina dei compleanni.
 */
@Component({
  selector: 'app-cliente-dialog',
  imports: [ReactiveFormsModule, Button, ButtonDirective, ButtonIcon, ButtonLabel, Dialog, InputText, Message, Select, Textarea],
  templateUrl: './cliente-dialog.html',
  styleUrl: './cliente-dialog.scss',
})
export class ClienteDialog {
  private readonly clienti = inject(ClientiService);
  private readonly fb = inject(FormBuilder);
  private readonly messaggi = inject(MessageService);
  private readonly conferma = inject(ConfirmationService);
  protected readonly auth = inject(AuthService);

  /** Finestra aperta o chiusa (two-way: [(visibile)]). */
  readonly visibile = model(false);
  /** Cliente da modificare; null per un nuovo cliente. */
  readonly clienteId = input<number | null>(null);
  /** Emesso dopo un salvataggio riuscito. */
  readonly salvato = output<Cliente>();
  /** Emesso dopo un'eliminazione riuscita. */
  readonly eliminato = output<number>();

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
  protected readonly erroriServer = signal<Record<string, string>>({});
  protected readonly titolo = computed(() => (this.clienteId() === null ? 'Nuovo cliente' : 'Modifica cliente'));
  private version = 0;

  constructor() {
    // A ogni apertura: modulo vuoto oppure caricato dal server.
    effect(() => {
      if (this.visibile()) {
        const id = this.clienteId();
        untracked(() => void this.prepara(id));
      }
    });
    // Un giorno che non esiste nel nuovo mese (es. 31 → febbraio) viene tolto.
    this.form.controls.meseNascita.valueChanges.subscribe(() => {
      const g = this.form.controls.giornoNascita.value;
      if (g && g > this.giorni().length) {
        this.form.controls.giornoNascita.setValue(null);
      }
    });
  }

  private async prepara(id: number | null): Promise<void> {
    this.form.reset();
    this.errore.set(null);
    this.erroriServer.set({});
    if (id === null) {
      return;
    }
    this.caricamento.set(true);
    try {
      const c = await this.clienti.dettaglio(id);
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
    const id = this.clienteId();
    const dati: ClienteRichiesta = { ...this.form.getRawValue() };
    this.invio.set(true);
    this.errore.set(null);
    this.erroriServer.set({});
    try {
      const c =
        id === null ? await this.clienti.crea(dati) : await this.clienti.modifica(id, { ...dati, version: this.version });
      this.messaggi.add({ severity: 'success', summary: id === null ? 'Cliente inserito' : 'Cliente aggiornato' });
      this.salvato.emit(c);
      this.visibile.set(false);
    } catch (err) {
      const campi = erroriCampo(err);
      this.erroriServer.set(campi);
      const conflitto = err instanceof HttpErrorResponse && err.status === 409;
      this.errore.set(
        Object.keys(campi).length
          ? 'Controlla i campi evidenziati.'
          : conflitto
            ? 'Nel frattempo un altro utente ha modificato questo cliente. Chiudi e riapri la scheda per vedere i dati aggiornati.'
            : messaggioErrore(err),
      );
    } finally {
      this.invio.set(false);
    }
  }

  chiediEliminazione(): void {
    const id = this.clienteId();
    if (id === null) return;
    const nome = `${this.form.controls.nome.value} ${this.form.controls.cognome.value}`;
    this.conferma.confirm({
      header: 'Eliminare il cliente?',
      message: `${nome} non comparirà più negli elenchi.`,
      icon: 'pi pi-exclamation-triangle',
      acceptButtonProps: { label: 'Elimina', severity: 'danger' },
      rejectButtonProps: { label: 'Annulla', outlined: true },
      accept: async () => {
        try {
          await this.clienti.elimina(id);
          this.messaggi.add({ severity: 'success', summary: 'Cliente eliminato' });
          this.eliminato.emit(id);
          this.visibile.set(false);
        } catch (err) {
          this.messaggi.add({ severity: 'error', summary: messaggioErrore(err) });
        }
      },
    });
  }

  /** Messaggio d'errore da mostrare sotto il campo, oppure null. */
  protected erroreCampo(campo: keyof typeof this.form.controls): string | null {
    const c = this.form.controls[campo];
    if (c.touched && c.invalid) {
      if (c.hasError('required')) return 'Obbligatorio';
      if (c.hasError('email')) return 'Email non valida';
    }
    return this.erroriServer()[campo] ?? null;
  }
}
