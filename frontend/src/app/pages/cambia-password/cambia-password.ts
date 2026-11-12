import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { erroriCampo, messaggioErrore } from '../../core/errori';
import { Avviso } from '../../core/ui/avviso';
import { Notifiche } from '../../core/ui/notifiche';

/** La conferma deve coincidere con la nuova password (errore sul campo "conferma"). */
function passwordUguali(group: AbstractControl): ValidationErrors | null {
  const nuova = group.get('nuovaPassword')?.value;
  const conferma = group.get('conferma');
  const diverse = !!nuova && !!conferma?.value && nuova !== conferma.value;
  if (conferma) {
    const { diverse: _, ...altri } = conferma.errors ?? {};
    conferma.setErrors(diverse ? { ...altri, diverse: true } : Object.keys(altri).length ? altri : null);
  }
  return null;
}

@Component({
  selector: 'app-cambia-password',
  imports: [ReactiveFormsModule, MatButton, MatFormFieldModule, MatIcon, MatInput, Avviso],
  templateUrl: './cambia-password.html',
  styleUrl: './cambia-password.scss',
})
export class CambiaPassword {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifiche = inject(Notifiche);

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
      this.notifiche.successo('Password aggiornata');
      await this.router.navigate(['/']);
    } catch (err) {
      const campi = erroriCampo(err);
      this.errore.set(Object.values(campi)[0] ?? messaggioErrore(err));
    } finally {
      this.invio.set(false);
    }
  }
}
