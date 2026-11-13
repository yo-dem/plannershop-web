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
import { Customer, CustomerRequest, CustomersApi } from '../../core/customers';
import { errorMessage, fieldErrors, markFields, saveErrorMessage } from '../../core/errors';
import { capitalize, monthNames } from '../../core/i18n';
import { Confirm } from '../../core/ui/confirm';
import { Messages } from '../../core/ui/messages';
import { Notice } from '../../core/ui/notice';
import { Notifications } from '../../core/ui/notifications';

/** Result of the dialog: the saved customer, "deleted", or undefined if cancelled. */
export type CustomerResult = Customer | 'deleted' | undefined;

/**
 * Opens the dialog to create (id = null) or edit a customer.
 * Used by the customer list and by the birthdays page.
 */
export async function openCustomer(dialog: MatDialog, id: number | null): Promise<CustomerResult> {
  const ref = dialog.open<CustomerDialog, number | null, CustomerResult>(CustomerDialog, {
    data: id,
    width: '40rem',
    maxWidth: '100vw',
    autoFocus: 'first-tabbable',
  });
  return firstValueFrom(ref.afterClosed());
}

type Field = 'firstName' | 'lastName' | 'phone' | 'email' | 'address' | 'notes' | 'birthMonth';

@Component({
  selector: 'app-customer-dialog',
  imports: [
    ReactiveFormsModule, MatButton, MatDialogModule, MatFormFieldModule, MatIcon, MatInput, MatProgressBar,
    MatSelectModule, Notice,
  ],
  templateUrl: './customer-dialog.html',
  styleUrl: './customer-dialog.scss',
})
export class CustomerDialog implements OnInit {
  private readonly customers = inject(CustomersApi);
  private readonly ref = inject<MatDialogRef<CustomerDialog, CustomerResult>>(MatDialogRef);
  private readonly notifications = inject(Notifications);
  private readonly confirm = inject(Confirm);
  protected readonly auth = inject(AuthService);
  /** Customer to edit; null for a new customer. */
  protected readonly id = inject<number | null>(MAT_DIALOG_DATA);

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    birthDay: this.fb.control<number | null>(null),
    birthMonth: this.fb.control<number | null>(null),
    phone: [''],
    email: ['', Validators.email],
    address: [''],
    notes: [''],
  });

  protected readonly months = monthNames().map((name, i) => ({ name: capitalize(name), value: i + 1 }));
  private readonly chosenMonth = toSignal(this.form.controls.birthMonth.valueChanges, { initialValue: null });
  /** Days of the chosen month (29 for February: the birthday has no year). */
  protected readonly days = computed(() => {
    const month = this.chosenMonth();
    const max = month ? new Date(2024, month, 0).getDate() : 31;
    return Array.from({ length: max }, (_, i) => i + 1);
  });

  protected readonly loading = signal(false);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Errors returned by the server, field → message. */
  private readonly serverErrors = signal<Record<string, string>>({});
  private version = 0;

  constructor() {
    // A day that does not exist in the new month (e.g. 31 → February) is removed.
    this.form.controls.birthMonth.valueChanges.subscribe(() => {
      const d = this.form.controls.birthDay.value;
      if (d && d > this.days().length) {
        this.form.controls.birthDay.setValue(null);
      }
    });
  }

  async ngOnInit(): Promise<void> {
    if (this.id === null) return;
    this.loading.set(true);
    try {
      const c = await this.customers.get(this.id);
      this.version = c.version;
      this.form.reset({
        firstName: c.firstName,
        lastName: c.lastName,
        birthDay: c.birthDay,
        birthMonth: c.birthMonth,
        phone: c.phone ?? '',
        email: c.email ?? '',
        address: c.address ?? '',
        notes: c.notes ?? '',
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
    const data: CustomerRequest = { ...this.form.getRawValue() };
    this.sending.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    try {
      const c =
        this.id === null
          ? await this.customers.create(data)
          : await this.customers.update(this.id, { ...data, version: this.version });
      this.notifications.success(
        this.id === null ? $localize`:@@customers.created:Customer added` : $localize`:@@customers.updated:Customer updated`,
      );
      this.ref.close(c);
    } catch (err) {
      const fields = fieldErrors(err);
      this.serverErrors.set(fields);
      markFields(fields, (f) => this.form.get(f === 'birthday' ? 'birthMonth' : f));
      this.error.set(saveErrorMessage(err, fields));
    } finally {
      this.sending.set(false);
    }
  }

  async delete(): Promise<void> {
    if (this.id === null) return;
    const { firstName, lastName } = this.form.getRawValue();
    const name = `${firstName} ${lastName}`;
    const ok = await this.confirm.ask({
      title: $localize`:@@customers.deleteTitle:Delete the customer?`,
      message: $localize`:@@customers.deleteMessage:${name}:name: will no longer appear in the lists.`,
      confirm: $localize`:@@common.delete:Delete`,
      danger: true,
    });
    if (!ok) return;
    try {
      await this.customers.delete(this.id);
      this.notifications.success($localize`:@@customers.deleted:Customer deleted`);
      this.ref.close('deleted');
    } catch (err) {
      this.notifications.error(errorMessage(err));
    }
  }

  /** Error message below the field (form or server validation). */
  protected message(field: Field): string {
    const server = this.serverErrors()[field === 'birthMonth' ? 'birthday' : field];
    if (server) return server;
    const c = this.form.controls[field];
    if (c.hasError('required')) return Messages.required();
    if (c.hasError('email')) return Messages.invalidEmail();
    return '';
  }
}
