import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { errorMessage, fieldErrors } from '../../core/errors';
import { Notice } from '../../core/ui/notice';
import { Notifications } from '../../core/ui/notifications';

/** The confirmation must match the new password (error on the "confirmation" field). */
function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const newPassword = group.get('newPassword')?.value;
  const confirmation = group.get('confirmation');
  const mismatch = !!newPassword && !!confirmation?.value && newPassword !== confirmation.value;
  if (confirmation) {
    const { mismatch: _, ...others } = confirmation.errors ?? {};
    confirmation.setErrors(mismatch ? { ...others, mismatch: true } : Object.keys(others).length ? others : null);
  }
  return null;
}

@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, MatButton, MatFormFieldModule, MatIcon, MatInput, Notice],
  templateUrl: './change-password.html',
  styleUrl: './change-password.scss',
})
export class ChangePassword {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(Notifications);

  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmation: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.sending.set(true);
    this.error.set(null);
    const { currentPassword, newPassword } = this.form.getRawValue();
    try {
      await this.auth.changePassword(currentPassword, newPassword);
      this.notifications.success($localize`:@@changePassword.done:Password updated`);
      await this.router.navigate(['/']);
    } catch (err) {
      this.error.set(Object.values(fieldErrors(err))[0] ?? errorMessage(err));
    } finally {
      this.sending.set(false);
    }
  }
}
