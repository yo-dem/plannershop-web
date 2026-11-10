import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { LoginResponse, Ruolo, UtenteCorrente } from './auth.models';

/**
 * Stato della sessione.
 *
 * - L'access token sta solo in memoria (non in localStorage): non è leggibile
 *   da script esterni e sparisce chiudendo la scheda.
 * - Il refresh token è in un cookie HttpOnly gestito dal browser: ricaricando
 *   la pagina, refresh() ripristina la sessione senza chiedere la password.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private accessToken: string | null = null;
  private refreshInCorso: Promise<boolean> | null = null;
  private readonly _utente = signal<UtenteCorrente | null>(null);

  readonly utente = this._utente.asReadonly();
  readonly autenticato = computed(() => this._utente() !== null);

  token(): string | null {
    return this.accessToken;
  }

  haRuolo(...ruoli: Ruolo[]): boolean {
    const u = this._utente();
    if (!u) return false;
    return ruoli.includes(u.ruolo) || (u.superadmin && ruoli.includes('SUPERADMIN'));
  }

  async login(email: string, password: string, tenantId?: number): Promise<LoginResponse> {
    const r = await firstValueFrom(
      this.http.post<LoginResponse>('/api/auth/login', { email, password, tenantId }),
    );
    if (r.accessToken) this.applica(r);
    return r;
  }

  /**
   * Rinnova l'access token usando il cookie. Se più richieste lo chiedono
   * insieme, la chiamata al server è una sola.
   */
  refresh(): Promise<boolean> {
    this.refreshInCorso ??= firstValueFrom(this.http.post<LoginResponse>('/api/auth/refresh', {}))
      .then((r) => {
        this.applica(r);
        return true;
      })
      .catch(() => {
        this.pulisci();
        return false;
      })
      .finally(() => (this.refreshInCorso = null));
    return this.refreshInCorso;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/auth/logout', {}));
    } catch {
      // anche se il server non risponde, lato client la sessione si chiude
    }
    this.pulisci();
    await this.router.navigate(['/login']);
  }

  /** Chiamato dall'interceptor quando la sessione non è più rinnovabile. */
  sessioneScaduta(): void {
    this.pulisci();
    void this.router.navigate(['/login'], { queryParams: { scaduta: 1 } });
  }

  async cambiaPassword(passwordAttuale: string, nuovaPassword: string): Promise<void> {
    await firstValueFrom(this.http.put('/api/auth/password', { passwordAttuale, nuovaPassword }));
    this._utente.update((u) => (u ? { ...u, deveCambiarePassword: false } : u));
  }

  private applica(r: LoginResponse): void {
    this.accessToken = r.accessToken;
    this._utente.set(r.utente);
  }

  private pulisci(): void {
    this.accessToken = null;
    this._utente.set(null);
  }
}
