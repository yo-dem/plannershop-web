import { Component, effect, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatProgressBar } from '@angular/material/progress-bar';

import { errorMessage, fieldErrors, markFields, saveErrorMessage } from '../../core/errors';
import { SettingsApi } from '../../core/settings';
import { Messages } from '../../core/ui/messages';
import { Notice } from '../../core/ui/notice';
import { Notifications } from '../../core/ui/notifications';

/** Same rule as the backend: the orange threshold cannot be below the red one. */
function thresholdsInOrder(group: AbstractControl): ValidationErrors | null {
  const out = group.get('outOfStockThreshold')?.value;
  const low = group.get('lowStockThreshold');
  const wrong = out != null && low?.value != null && low.value < out;
  if (low) {
    const { order: _, ...others } = low.errors ?? {};
    low.setErrors(wrong ? { ...others, order: true } : Object.keys(others).length ? others : null);
  }
  return null;
}

/** Settings of the business, changed by its ADMIN. For now: stock thresholds of the products list. */
@Component({
  selector: 'app-settings',
  imports: [ReactiveFormsModule, MatButton, MatFormFieldModule, MatIcon, MatInput, MatProgressBar, Notice],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
})
export class Settings {
  protected readonly settings = inject(SettingsApi);
  private readonly notifications = inject(Notifications);

  private readonly integer = [Validators.required, Validators.min(0), Validators.max(1_000_000), Validators.pattern(/^\d+$/)];
  protected readonly form = inject(FormBuilder).group(
    {
      outOfStockThreshold: [null as number | null, this.integer],
      lowStockThreshold: [null as number | null, this.integer],
    },
    { validators: thresholdsInOrder },
  );

  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});

  constructor() {
    // Fill the form with the current values (also after a save or a reload).
    effect(() => {
      const s = this.settings.value();
      if (!this.form.dirty) {
        this.form.reset({ outOfStockThreshold: s.outOfStockThreshold, lowStockThreshold: s.lowStockThreshold });
      }
    });
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.sending.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    const { outOfStockThreshold, lowStockThreshold } = this.form.getRawValue();
    try {
      await this.settings.save({
        outOfStockThreshold: outOfStockThreshold!,
        lowStockThreshold: lowStockThreshold!,
        version: this.settings.value().version,
      });
      this.form.markAsPristine();
      this.form.reset(this.form.getRawValue());
      this.notifications.success($localize`:@@settings.saved:Settings saved`);
    } catch (err) {
      const fields = fieldErrors(err);
      this.serverErrors.set(fields);
      markFields(fields, (f) => this.form.get(f));
      this.error.set(saveErrorMessage(err, fields));
      if (!Object.keys(fields).length) this.settings.reload();
    } finally {
      this.sending.set(false);
    }
  }

  /** Error under a threshold field. */
  protected fieldError(name: 'outOfStockThreshold' | 'lowStockThreshold'): string {
    const c = this.form.controls[name];
    if (c.hasError('server')) return this.serverErrors()[name] ?? '';
    if (c.hasError('required')) return Messages.required();
    if (c.hasError('order')) return $localize`:@@settings.orderError:Cannot be lower than the red threshold`;
    return $localize`:@@settings.integerError:Enter a whole number from 0 up`;
  }

  protected readonly errorMessage = errorMessage;
}
