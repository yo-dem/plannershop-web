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
import { Prodotto, ProdottiService } from '../../core/prodotti';
import { Avviso } from '../../core/ui/avviso';
import { CampiPrezzo } from '../../core/ui/campi-prezzo';
import { Conferma } from '../../core/ui/conferma';
import { Notifiche } from '../../core/ui/notifiche';

/** Esito della finestra: il prodotto salvato, "eliminato", oppure undefined se annullata. */
export type EsitoProdotto = Prodotto | 'eliminato' | undefined;

/** Apre la finestra per inserire (id = null) o modificare un prodotto. */
export async function apriProdotto(dialog: MatDialog, id: number | null): Promise<EsitoProdotto> {
  const ref = dialog.open<ProdottoDialog, number | null, EsitoProdotto>(ProdottoDialog, {
    data: id,
    width: '44rem',
    maxWidth: '100vw',
    autoFocus: 'first-tabbable',
  });
  return firstValueFrom(ref.afterClosed());
}

type Campo = 'marca' | 'descrizione' | 'quantita' | 'dataCarico' | 'note';

/** Oggi nel formato dei campi data ("2026-03-15"), nel fuso orario del browser. */
function oggi(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

@Component({
  selector: 'app-prodotto-dialog',
  imports: [
    ReactiveFormsModule, MatButton, MatDialogModule, MatFormFieldModule, MatIcon, MatInput, MatProgressBar, Avviso,
    CampiPrezzo,
  ],
  templateUrl: './prodotto-dialog.html',
})
export class ProdottoDialog implements OnInit {
  private readonly prodotti = inject(ProdottiService);
  private readonly ref = inject<MatDialogRef<ProdottoDialog, EsitoProdotto>>(MatDialogRef);
  private readonly notifiche = inject(Notifiche);
  private readonly conferma = inject(Conferma);
  /** Prodotto da modificare; null per un nuovo prodotto. */
  protected readonly id = inject<number | null>(MAT_DIALOG_DATA);

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.nonNullable.group({
    marca: ['', Validators.required],
    descrizione: ['', Validators.required],
    quantita: this.fb.control<number | null>(null, [Validators.required, Validators.min(0), Validators.pattern(/^\d+$/)]),
    dataCarico: [oggi()],
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
      const p = await this.prodotti.dettaglio(this.id);
      this.version = p.version;
      this.form.reset({
        marca: p.marca,
        descrizione: p.descrizione,
        quantita: p.quantita,
        dataCarico: p.dataCarico ?? '',
        prezzi: valoriPrezzi(p),
        note: p.note ?? '',
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
      marca: v.marca,
      descrizione: v.descrizione,
      quantita: v.quantita ?? 0,
      dataCarico: v.dataCarico || null,
      note: v.note,
      ...richiestaPrezzi(this.form.controls.prezzi),
    };
    this.invio.set(true);
    this.errore.set(null);
    this.erroriServer.set({});
    try {
      const p =
        this.id === null
          ? await this.prodotti.crea(dati)
          : await this.prodotti.modifica(this.id, { ...dati, version: this.version });
      this.notifiche.successo(this.id === null ? 'Prodotto inserito' : 'Prodotto aggiornato');
      this.ref.close(p);
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
    const { marca, descrizione } = this.form.getRawValue();
    const ok = await this.conferma.chiedi({
      titolo: 'Eliminare il prodotto?',
      messaggio: `${marca} – ${descrizione} non comparirà più negli elenchi. Le vendite passate restano invariate.`,
      conferma: 'Elimina',
      pericolo: true,
    });
    if (!ok) return;
    try {
      await this.prodotti.elimina(this.id);
      this.notifiche.successo('Prodotto eliminato');
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
    if (c.hasError('min') || c.hasError('pattern')) return 'Numero intero, zero o più';
    return '';
  }
}
