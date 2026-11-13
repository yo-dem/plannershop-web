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
import { formatIban, ibanValidator } from '../../core/iban';
import { Supplier, SuppliersApi } from '../../core/suppliers';
import { Confirm } from '../../core/ui/confirm';
import { Messages } from '../../core/ui/messages';
import { Notice } from '../../core/ui/notice';
import { Notifications } from '../../core/ui/notifications';

/** Result of the dialog: the saved supplier, "deleted", or undefined if cancelled. */
export type SupplierResult = Supplier | 'deleted' | undefined;

/** Opens the dialog to create (id = null) or edit a supplier. */
export async function openSupplier(dialog: MatDialog, id: number | null): Promise<SupplierResult> {
  const ref = dialog.open<SupplierDialog, number | null, SupplierResult>(SupplierDialog, {
    data: id,
    width: '40rem',
    maxWidth: '100vw',
    autoFocus: 'first-tabbable',
  });
  return firstValueFrom(ref.afterClosed());
}

type Field = 'name' | 'address' | 'phone' | 'email' | 'iban' | 'notes';

@Component({
  selector: 'app-supplier-dialog',
  imports: [ReactiveFormsModule, MatButton, MatDialogModule, MatFormFieldModule, MatIcon, MatInput, MatProgressBar, Notice],
  templateUrl: './supplier-dialog.html',
})
export class SupplierDialog implements OnInit {
  private readonly suppliers = inject(SuppliersApi);
  private readonly ref = inject<MatDialogRef<SupplierDialog, SupplierResult>>(MatDialogRef);
  private readonly notifications = inject(Notifications);
  private readonly confirm = inject(Confirm);
  /** Supplier to edit; null for a new supplier. */
  protected readonly id = inject<number | null>(MAT_DIALOG_DATA);

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    address: ['', Validators.required],
    phone: ['', Validators.required],
    email: ['', Validators.email],
    iban: ['', ibanValidator],
    notes: [''],
  });

  protected readonly loading = signal(false);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Errors returned by the server, field → message. */
  private readonly serverErrors = signal<Record<string, string>>({});
  private version = 0;

  async ngOnInit(): Promise<void> {
    if (this.id === null) return;
    this.loading.set(true);
    try {
      const s = await this.suppliers.get(this.id);
      this.version = s.version;
      this.form.reset({
        name: s.name,
        address: s.address,
        phone: s.phone,
        email: s.email ?? '',
        iban: formatIban(s.iban),
        notes: s.notes ?? '',
      });
    } catch (err) {
      this.error.set(errorMessage(err));
    } finally {
      this.loading.set(false);
    }
  }

  /** When leaving the field, a valid IBAN is rewritten in groups of four. */
  protected formatIban(): void {
    const c = this.form.controls.iban;
    if (c.valid && c.value) c.setValue(formatIban(c.value));
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const data = this.form.getRawValue();
    this.sending.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    try {
      const s =
        this.id === null
          ? await this.suppliers.create(data)
          : await this.suppliers.update(this.id, { ...data, version: this.version });
      this.notifications.success(
        this.id === null ? $localize`:@@suppliers.created:Supplier added` : $localize`:@@suppliers.updated:Supplier updated`,
      );
      this.ref.close(s);
    } catch (err) {
      const fields = fieldErrors(err);
      this.serverErrors.set(fields);
      markFields(fields, (f) => this.form.get(f));
      this.error.set(saveErrorMessage(err, fields));
    } finally {
      this.sending.set(false);
    }
  }

  async delete(): Promise<void> {
    if (this.id === null) return;
    const name = this.form.controls.name.value;
    const ok = await this.confirm.ask({
      title: $localize`:@@suppliers.deleteTitle:Delete the supplier?`,
      message: $localize`:@@suppliers.deleteMessage:${name}:name: will no longer appear in the lists.`,
      confirm: $localize`:@@common.delete:Delete`,
      danger: true,
    });
    if (!ok) return;
    try {
      await this.suppliers.delete(this.id);
      this.notifications.success($localize`:@@suppliers.deleted:Supplier deleted`);
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
    if (c.hasError('email')) return Messages.invalidEmail();
    if (c.hasError('iban')) return $localize`:@@suppliers.invalidIban:Invalid IBAN: check the digits`;
    return '';
  }
}
