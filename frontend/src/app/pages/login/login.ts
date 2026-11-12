import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';

import { TenantOpzione } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { messaggioErrore } from '../../core/errori';
import { Avviso } from '../../core/ui/avviso';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatButton, MatIconButton, MatFormFieldModule, MatIcon, MatInput, MatSelectModule, Avviso],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Parametri dell'URL (withComponentInputBinding). */
  readonly returnUrl = input<string>();
  readonly scaduta = input<string>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    tenantId: [null as number | null],
  });

  protected readonly invio = signal(false);
  protected readonly errore = signal<string | null>(null);
  protected readonly mostraPassword = signal(false);
  /** Attività tra cui scegliere, se l'utente ne ha più di una. */
  protected readonly attivita = signal<TenantOpzione[] | null>(null);

  async accedi(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.invio.set(true);
    this.errore.set(null);
    const { email, password, tenantId } = this.form.getRawValue();
    try {
      const r = await this.auth.login(email, password, tenantId ?? undefined);
      if (r.sceltaTenant) {
        this.attivita.set(r.sceltaTenant);
        this.form.controls.tenantId.setValidators(Validators.required);
        this.form.controls.tenantId.updateValueAndValidity();
        return;
      }
      const u = r.utente!;
      const destinazione = u.deveCambiarePassword
        ? '/cambia-password'
        : (this.returnUrl() ?? (u.tenant ? '/' : '/piattaforma'));
      await this.router.navigateByUrl(destinazione);
    } catch (err) {
      this.errore.set(messaggioErrore(err));
    } finally {
      this.invio.set(false);
    }
  }
}
