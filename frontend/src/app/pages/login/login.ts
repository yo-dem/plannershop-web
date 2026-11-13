import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';

import { TenantOption } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/errors';
import { Notice } from '../../core/ui/notice';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatButton, MatIconButton, MatFormFieldModule, MatIcon, MatInput, MatSelectModule, Notice],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** URL parameters (withComponentInputBinding). */
  readonly returnUrl = input<string>();
  readonly expired = input<string>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    tenantId: [null as number | null],
  });

  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showPassword = signal(false);
  /** Tenants to choose from, if the user belongs to more than one. */
  protected readonly tenants = signal<TenantOption[] | null>(null);

  async signIn(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.sending.set(true);
    this.error.set(null);
    const { email, password, tenantId } = this.form.getRawValue();
    try {
      const r = await this.auth.login(email, password, tenantId ?? undefined);
      if (r.tenantChoices) {
        this.tenants.set(r.tenantChoices);
        this.form.controls.tenantId.setValidators(Validators.required);
        this.form.controls.tenantId.updateValueAndValidity();
        return;
      }
      const u = r.user!;
      const target = u.mustChangePassword ? '/change-password' : (this.returnUrl() ?? (u.tenant ? '/' : '/platform'));
      await this.router.navigateByUrl(target);
    } catch (err) {
      this.error.set(errorMessage(err));
    } finally {
      this.sending.set(false);
    }
  }
}
