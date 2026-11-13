import { Component, Injector, OnInit, Signal, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { startWith } from 'rxjs';

import { COMMON_VAT_RATES, PriceBase, PriceGroup, recalculate } from '../prices';
import { Messages } from './messages';

type PriceField = 'vatRate' | 'netPrice' | 'grossPrice' | 'salePrice';

/**
 * Price fields of products and services: VAT rate, net price, gross price
 * (VAT included) and sale price. The user types the net or the gross price:
 * the other one is calculated (SPECIFICA §7.1).
 *
 * Usage: <app-price-fields [group]="form.controls.prices" [serverErrors]="serverErrors()" />
 */
@Component({
  selector: 'app-price-fields',
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatFormFieldModule, MatInput],
  template: `
    <div class="prices" [formGroup]="group()">
      <mat-form-field>
        <mat-label i18n="@@prices.vatRate">VAT</mat-label>
        <input matInput id="vatRate" formControlName="vatRate" inputmode="decimal" autocomplete="off"
               [matAutocomplete]="rates" (input)="recalculate()" required />
        <span matTextSuffix>%</span>
        <mat-autocomplete #rates (optionSelected)="recalculate()">
          @for (r of commonRates; track r) {
            <mat-option [value]="'' + r">{{ r }}%</mat-option>
          }
        </mat-autocomplete>
        <mat-error>{{ message('vatRate') }}</mat-error>
      </mat-form-field>

      <mat-form-field>
        <mat-label i18n="@@prices.net">Net price</mat-label>
        <input matInput id="netPrice" formControlName="netPrice" inputmode="decimal" autocomplete="off"
               (input)="entered('NET')" required />
        <span matTextSuffix>€</span>
        @if (priceBase() === 'GROSS') {
          <mat-hint i18n="@@prices.calculated">Calculated</mat-hint>
        }
        <mat-error>{{ message('netPrice') }}</mat-error>
      </mat-form-field>

      <mat-form-field>
        <mat-label i18n="@@prices.gross">Price incl. VAT</mat-label>
        <input matInput id="grossPrice" formControlName="grossPrice" inputmode="decimal" autocomplete="off"
               (input)="entered('GROSS')" required />
        <span matTextSuffix>€</span>
        @if (priceBase() === 'NET') {
          <mat-hint i18n="@@prices.calculated">Calculated</mat-hint>
        }
        <mat-error>{{ message('grossPrice') }}</mat-error>
      </mat-form-field>
    </div>

    <mat-form-field class="sale" [formGroup]="group()">
      <mat-label i18n="@@prices.sale">Sale price</mat-label>
      <input matInput id="salePrice" formControlName="salePrice" inputmode="decimal" autocomplete="off" required />
      <span matTextSuffix>€</span>
      <mat-hint i18n="@@prices.saleHint">The price offered to the customer</mat-hint>
      <mat-error>{{ message('salePrice') }}</mat-error>
    </mat-form-field>
  `,
  styles: `
    :host {
      display: grid;
      gap: 0.875rem;
    }
    .prices {
      display: grid;
      grid-template-columns: 7rem 1fr 1fr;
      gap: 0.875rem 0.75rem;
    }
    @media (max-width: 640px) {
      .prices {
        grid-template-columns: 1fr 1fr;
      }
      .prices > :first-child {
        grid-column: 1 / -1;
      }
    }
  `,
})
export class PriceFields implements OnInit {
  readonly group = input.required<PriceGroup>();
  /** Errors returned by the server, field → message ("price" = the entered price). */
  readonly serverErrors = input<Record<string, string>>({});

  protected readonly commonRates = COMMON_VAT_RATES;
  protected priceBase!: Signal<PriceBase>;
  private readonly injector = inject(Injector);

  ngOnInit(): void {
    // Follows also the values loaded later (editing a saved item).
    const c = this.group().controls.priceBase;
    this.priceBase = toSignal(c.valueChanges.pipe(startWith(c.value)), {
      injector: this.injector,
      requireSync: true,
    });
  }

  /** The user typed one of the two prices: it becomes the one the other is calculated from. */
  protected entered(base: PriceBase): void {
    this.group().controls.priceBase.setValue(base);
    this.recalculate();
  }

  protected recalculate(): void {
    recalculate(this.group());
  }

  protected message(field: PriceField): string {
    const g = this.group();
    const entered = g.controls.priceBase.value === 'NET' ? 'netPrice' : 'grossPrice';
    const server = this.serverErrors()[field] ?? (field === entered ? this.serverErrors()['price'] : undefined);
    if (server) return server;
    const c = g.controls[field];
    if (c.hasError('required')) return field === entered || field === 'salePrice' ? Messages.required() : '';
    if (c.hasError('amount')) return $localize`:@@prices.invalidAmount:Enter an amount, e.g. 18.50`;
    if (c.hasError('vatRate')) return $localize`:@@prices.invalidVatRate:From 0 to 100`;
    return '';
  }
}
