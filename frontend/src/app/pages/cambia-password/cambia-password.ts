import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';

import { AuthService } from '../../core/auth/auth.service';
import { erroriCampo, messaggioErrore } from '../../core/errori';

/** La conferma deve coincidere con la nuova password. */
function passwordUguali(group: AbstractControl): ValidationErrors | null {
  const nuova = group.get('nuovaPassword')?.value;
  const conferma = group.get('conferma')?.value;
  return nuova && conferma && nuova !== conferma ? { diverse: true } : null;
}

@Component({
  selector: 'app-cambia-password',
  imports: [ReactiveFormsModule, Button, Message, Password],
  templateUrl: './cambia-password.html',
  styleUrl: './cambia-password.scss',
})
export class CambiaPassword {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly messaggi = inject(MessageService);

  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      passwordAttuale: ['', Validators.required],
      nuovaPassword: ['', [Validators.required, Validators.minLength(8)]],
      conferma: ['', Validators.required],
    },
    { validators: passwordUguali },
  );

  protected readonly invio = signal(false);
  protected readonly errore = signal<string | null>(null);

  async salva(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.invio.set(true);
    this.errore.set(null);
    const { passwordAttuale, nuovaPassword } = this.form.getRawValue();
    try {
      await this.auth.cambiaPassword(passwordAttuale, nuovaPassword);
      this.messaggi.add({ severity: 'success', summary: 'Password aggiornata' });
      await this.router.navigate(['/']);
    } catch (err) {
      const campi = erroriCampo(err);
      this.errore.set(Object.values(campi)[0] ?? messaggioErrore(err));
    } finally {
      this.invio.set(false);
    }
  }

  protected nonValido(campo: 'passwordAttuale' | 'nuovaPassword' | 'conferma'): boolean {
    const c = this.form.controls[campo];
    return c.invalid && c.touched;
  }
}
