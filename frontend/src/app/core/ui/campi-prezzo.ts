import { Component, Injector, OnInit, Signal, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { startWith } from 'rxjs';

import { ALIQUOTE_COMUNI, CalcolaDa, GruppoPrezzi, ricalcola } from '../prezzi';

type CampoPrezzo = 'aliquotaIva' | 'prezzoNetto' | 'prezzoIvato' | 'prezzoVendita';

/**
 * Campi dei prezzi di prodotti e servizi: aliquota IVA, prezzo netto, prezzo
 * IVA inclusa e prezzo di vendita. Si scrive il netto oppure l'ivato: l'altro
 * si calcola da solo (SPECIFICA §7.1).
 *
 * Uso: <app-campi-prezzo [gruppo]="form.controls.prezzi" [erroriServer]="erroriServer()" />
 */
@Component({
  selector: 'app-campi-prezzo',
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatFormFieldModule, MatInput],
  template: `
    <div class="prezzi" [formGroup]="gruppo()">
      <mat-form-field>
        <mat-label>IVA</mat-label>
        <input matInput id="aliquotaIva" formControlName="aliquotaIva" inputmode="decimal" autocomplete="off"
               [matAutocomplete]="aliquote" (input)="ricalcola()" required />
        <span matTextSuffix>%</span>
        <mat-autocomplete #aliquote (optionSelected)="ricalcola()">
          @for (a of aliquoteComuni; track a) {
            <mat-option [value]="'' + a">{{ a }}%</mat-option>
          }
        </mat-autocomplete>
        <mat-error>{{ messaggio('aliquotaIva') }}</mat-error>
      </mat-form-field>

      <mat-form-field>
        <mat-label>Prezzo netto</mat-label>
        <input matInput id="prezzoNetto" formControlName="prezzoNetto" inputmode="decimal" autocomplete="off"
               (input)="inserito('NETTO')" required />
        <span matTextSuffix>€</span>
        @if (calcolaDa() === 'IVATO') {
          <mat-hint>Calcolato</mat-hint>
        }
        <mat-error>{{ messaggio('prezzoNetto') }}</mat-error>
      </mat-form-field>

      <mat-form-field>
        <mat-label>Prezzo IVA inclusa</mat-label>
        <input matInput id="prezzoIvato" formControlName="prezzoIvato" inputmode="decimal" autocomplete="off"
               (input)="inserito('IVATO')" required />
        <span matTextSuffix>€</span>
        @if (calcolaDa() === 'NETTO') {
          <mat-hint>Calcolato</mat-hint>
        }
        <mat-error>{{ messaggio('prezzoIvato') }}</mat-error>
      </mat-form-field>
    </div>

    <mat-form-field class="vendita" [formGroup]="gruppo()">
      <mat-label>Prezzo di vendita</mat-label>
      <input matInput id="prezzoVendita" formControlName="prezzoVendita" inputmode="decimal" autocomplete="off"
             required />
      <span matTextSuffix>€</span>
      <mat-hint>Il prezzo proposto al cliente</mat-hint>
      <mat-error>{{ messaggio('prezzoVendita') }}</mat-error>
    </mat-form-field>
  `,
  styles: `
    :host {
      display: grid;
      gap: 0.875rem;
    }
    .prezzi {
      display: grid;
      grid-template-columns: 7rem 1fr 1fr;
      gap: 0.875rem 0.75rem;
    }
    @media (max-width: 640px) {
      .prezzi {
        grid-template-columns: 1fr 1fr;
      }
      .prezzi > :first-child {
        grid-column: 1 / -1;
      }
    }
  `,
})
export class CampiPrezzo implements OnInit {
  readonly gruppo = input.required<GruppoPrezzi>();
  /** Errori restituiti dal server, campo → messaggio ("prezzo" = il prezzo inserito). */
  readonly erroriServer = input<Record<string, string>>({});

  protected readonly aliquoteComuni = ALIQUOTE_COMUNI;
  protected calcolaDa!: Signal<CalcolaDa>;
  private readonly injector = inject(Injector);

  ngOnInit(): void {
    // Segue anche i valori caricati dopo (modifica di un articolo salvato).
    const c = this.gruppo().controls.calcolaDa;
    this.calcolaDa = toSignal(c.valueChanges.pipe(startWith(c.value)), {
      injector: this.injector,
      requireSync: true,
    });
  }

  /** L'utente ha scritto in uno dei due prezzi: diventa quello da cui si calcola l'altro. */
  protected inserito(da: CalcolaDa): void {
    this.gruppo().controls.calcolaDa.setValue(da);
    this.ricalcola();
  }

  protected ricalcola(): void {
    ricalcola(this.gruppo());
  }

  protected messaggio(campo: CampoPrezzo): string {
    const g = this.gruppo();
    const inserito = g.controls.calcolaDa.value === 'NETTO' ? 'prezzoNetto' : 'prezzoIvato';
    const server = this.erroriServer()[campo] ?? (campo === inserito ? this.erroriServer()['prezzo'] : undefined);
    if (server) return server;
    const c = g.controls[campo];
    if (c.hasError('required')) return campo === inserito || campo === 'prezzoVendita' ? 'Obbligatorio' : '';
    if (c.hasError('importo')) return 'Scrivi un importo, es. 18,50';
    if (c.hasError('aliquota')) return 'Da 0 a 100';
    return '';
  }
}
