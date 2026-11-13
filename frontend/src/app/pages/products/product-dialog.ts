import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatProgressBar } from '@angular/material/progress-bar';
import { firstValueFrom } from 'rxjs';

import { errorMessage, fieldErrors, markFields, saveErrorMessage } from '../../core/errors';
import { priceGroup, priceRequest, priceValues } from '../../core/prices';
import { Product, ProductsApi } from '../../core/products';
import { Confirm } from '../../core/ui/confirm';
import { Messages } from '../../core/ui/messages';
import { Notice } from '../../core/ui/notice';
import { Notifications } from '../../core/ui/notifications';
import { PriceFields } from '../../core/ui/price-fields';

/** Result of the dialog: the saved product, "deleted", or undefined if cancelled. */
export type ProductResult = Product | 'deleted' | undefined;

/** Opens the dialog to create (id = null) or edit a product. */
export async function openProduct(dialog: MatDialog, id: number | null): Promise<ProductResult> {
  const ref = dialog.open<ProductDialog, number | null, ProductResult>(ProductDialog, {
    data: id,
    width: '44rem',
    maxWidth: '100vw',
    autoFocus: 'first-tabbable',
  });
  return firstValueFrom(ref.afterClosed());
}

type Field = 'brand' | 'description' | 'quantity' | 'stockDate' | 'notes';

/** Today in the format of date fields ("2026-03-15"), in the browser's time zone. */
function today(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** The "price" error of the server refers to the price the user entered. */
export function priceField(field: string, base: 'NET' | 'GROSS'): string {
  if (field !== 'price') return field;
  return base === 'NET' ? 'netPrice' : 'grossPrice';
}

@Component({
  selector: 'app-product-dialog',
  imports: [
    ReactiveFormsModule, MatButton, MatDialogModule, MatFormFieldModule, MatIcon, MatInput, MatProgressBar, Notice,
    PriceFields,
  ],
  templateUrl: './product-dialog.html',
})
export class ProductDialog implements OnInit {
  private readonly products = inject(ProductsApi);
  private readonly ref = inject<MatDialogRef<ProductDialog, ProductResult>>(MatDialogRef);
  private readonly notifications = inject(Notifications);
  private readonly confirm = inject(Confirm);
  /** Product to edit; null for a new product. */
  protected readonly id = inject<number | null>(MAT_DIALOG_DATA);

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.nonNullable.group({
    brand: ['', Validators.required],
    description: ['', Validators.required],
    quantity: this.fb.control<number | null>(null, [Validators.required, Validators.min(0), Validators.pattern(/^\d+$/)]),
    stockDate: [today()],
    prices: priceGroup(this.fb),
    notes: [''],
  });

  protected readonly loading = signal(false);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Errors returned by the server, field → message. */
  protected readonly serverErrors = signal<Record<string, string>>({});
  private version = 0;

  async ngOnInit(): Promise<void> {
    if (this.id === null) return;
    this.loading.set(true);
    try {
      const p = await this.products.get(this.id);
      this.version = p.version;
      this.form.reset({
        brand: p.brand,
        description: p.description,
        quantity: p.quantity,
        stockDate: p.stockDate ?? '',
        prices: priceValues(p),
        notes: p.notes ?? '',
      });
    } catch (err) {
      this.error.set(errorMessage(err));
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const data = {
      brand: v.brand,
      description: v.description,
      quantity: v.quantity ?? 0,
      stockDate: v.stockDate || null,
      notes: v.notes,
      ...priceRequest(this.form.controls.prices),
    };
    this.sending.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    try {
      const p =
        this.id === null
          ? await this.products.create(data)
          : await this.products.update(this.id, { ...data, version: this.version });
      this.notifications.success(
        this.id === null ? $localize`:@@products.created:Product added` : $localize`:@@products.updated:Product updated`,
      );
      this.ref.close(p);
    } catch (err) {
      const fields = fieldErrors(err);
      this.serverErrors.set(fields);
      const base = this.form.controls.prices.controls.priceBase.value;
      markFields(fields, (f) => this.form.get(f) ?? this.form.controls.prices.get(priceField(f, base)));
      this.error.set(saveErrorMessage(err, fields));
    } finally {
      this.sending.set(false);
    }
  }

  async delete(): Promise<void> {
    if (this.id === null) return;
    const { brand, description } = this.form.getRawValue();
    const name = `${brand} – ${description}`;
    const ok = await this.confirm.ask({
      title: $localize`:@@products.deleteTitle:Delete the product?`,
      message: $localize`:@@products.deleteMessage:${name}:name: will no longer appear in the lists. Past sales stay unchanged.`,
      confirm: $localize`:@@common.delete:Delete`,
      danger: true,
    });
    if (!ok) return;
    try {
      await this.products.delete(this.id);
      this.notifications.success($localize`:@@products.deleted:Product deleted`);
      this.ref.close('deleted');
    } catch (err) {
      this.notifications.error(errorMessage(err));
    }
  }

  protected message(field: Field): string {
    const server = this.serverErrors()[field];
    if (server) return server;
    const c = this.form.controls[field];
    if (c.hasError('required')) return Messages.required();
    if (c.hasError('min') || c.hasError('pattern')) return $localize`:@@products.quantityError:Whole number, zero or more`;
    return '';
  }
}
