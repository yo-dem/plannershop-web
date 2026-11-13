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
import { ServiceItem, ServicesApi } from '../../core/services';
import { Confirm } from '../../core/ui/confirm';
import { Messages } from '../../core/ui/messages';
import { Notice } from '../../core/ui/notice';
import { Notifications } from '../../core/ui/notifications';
import { PriceFields } from '../../core/ui/price-fields';
import { priceField } from '../products/product-dialog';

/** Result of the dialog: the saved service, "deleted", or undefined if cancelled. */
export type ServiceResult = ServiceItem | 'deleted' | undefined;

/** Opens the dialog to create (id = null) or edit a service. */
export async function openService(dialog: MatDialog, id: number | null): Promise<ServiceResult> {
  const ref = dialog.open<ServiceDialog, number | null, ServiceResult>(ServiceDialog, {
    data: id,
    width: '40rem',
    maxWidth: '100vw',
    autoFocus: 'first-tabbable',
  });
  return firstValueFrom(ref.afterClosed());
}

type Field = 'name' | 'description' | 'notes';

@Component({
  selector: 'app-service-dialog',
  imports: [
    ReactiveFormsModule, MatButton, MatDialogModule, MatFormFieldModule, MatIcon, MatInput, MatProgressBar, Notice,
    PriceFields,
  ],
  templateUrl: './service-dialog.html',
})
export class ServiceDialog implements OnInit {
  private readonly services = inject(ServicesApi);
  private readonly ref = inject<MatDialogRef<ServiceDialog, ServiceResult>>(MatDialogRef);
  private readonly notifications = inject(Notifications);
  private readonly confirm = inject(Confirm);
  /** Service to edit; null for a new service. */
  protected readonly id = inject<number | null>(MAT_DIALOG_DATA);

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: ['', Validators.required],
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
      const s = await this.services.get(this.id);
      this.version = s.version;
      this.form.reset({
        name: s.name,
        description: s.description,
        prices: priceValues(s),
        notes: s.notes ?? '',
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
    const data = { name: v.name, description: v.description, notes: v.notes, ...priceRequest(this.form.controls.prices) };
    this.sending.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    try {
      const s =
        this.id === null
          ? await this.services.create(data)
          : await this.services.update(this.id, { ...data, version: this.version });
      this.notifications.success(
        this.id === null ? $localize`:@@services.created:Service added` : $localize`:@@services.updated:Service updated`,
      );
      this.ref.close(s);
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
    const name = this.form.controls.name.value;
    const ok = await this.confirm.ask({
      title: $localize`:@@services.deleteTitle:Delete the service?`,
      message: $localize`:@@services.deleteMessage:${name}:name: will no longer appear in the lists. Past sales stay unchanged.`,
      confirm: $localize`:@@common.delete:Delete`,
      danger: true,
    });
    if (!ok) return;
    try {
      await this.services.delete(this.id);
      this.notifications.success($localize`:@@services.deleted:Service deleted`);
      this.ref.close('deleted');
    } catch (err) {
      this.notifications.error(errorMessage(err));
    }
  }

  protected message(field: Field): string {
    const server = this.serverErrors()[field];
    if (server) return server;
    return this.form.controls[field].hasError('required') ? Messages.required() : '';
  }
}
