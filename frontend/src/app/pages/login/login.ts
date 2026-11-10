import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';
import { Select } from 'primeng/select';

import { TenantOpzione } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { messaggioErrore } from '../../core/errori';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, Button, InputText, Message, Password, Select],
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

  protected nonValido(campo: 'email' | 'password' | 'tenantId'): boolean {
    const c = this.form.controls[campo];
    return c.invalid && c.touched;
  }
}
