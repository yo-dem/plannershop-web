import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { CurrentUser, LoginResponse, Role } from './auth.models';

/**
 * Session state.
 *
 * - The access token lives only in memory (not in localStorage): external
 *   scripts cannot read it and it disappears when the tab is closed.
 * - The refresh token is in an HttpOnly cookie handled by the browser: when the
 *   page is reloaded, refresh() restores the session without asking for the password.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private accessToken: string | null = null;
  private refreshInProgress: Promise<boolean> | null = null;
  private readonly _user = signal<CurrentUser | null>(null);

  readonly user = this._user.asReadonly();
  readonly authenticated = computed(() => this._user() !== null);

  token(): string | null {
    return this.accessToken;
  }

  hasRole(...roles: Role[]): boolean {
    const u = this._user();
    if (!u) return false;
    return roles.includes(u.role) || (u.superadmin && roles.includes('SUPERADMIN'));
  }

  async login(email: string, password: string, tenantId?: number): Promise<LoginResponse> {
    const r = await firstValueFrom(
      this.http.post<LoginResponse>('/api/auth/login', { email, password, tenantId }),
    );
    if (r.accessToken) this.apply(r);
    return r;
  }

  /**
   * Renews the access token using the cookie. If several requests ask for it
   * at the same time, only one call reaches the server.
   */
  refresh(): Promise<boolean> {
    this.refreshInProgress ??= firstValueFrom(this.http.post<LoginResponse>('/api/auth/refresh', {}))
      .then((r) => {
        this.apply(r);
        return true;
      })
      .catch(() => {
        this.clear();
        return false;
      })
      .finally(() => (this.refreshInProgress = null));
    return this.refreshInProgress;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/auth/logout', {}));
    } catch {
      // even if the server does not answer, the session is closed on the client
    }
    this.clear();
    await this.router.navigate(['/login']);
  }

  /** Called by the interceptor when the session can no longer be renewed. */
  sessionExpired(): void {
    this.clear();
    void this.router.navigate(['/login'], { queryParams: { expired: 1 } });
  }

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await firstValueFrom(this.http.put('/api/auth/password', { currentPassword, newPassword }));
    this._user.update((u) => (u ? { ...u, mustChangePassword: false } : u));
  }

  private apply(r: LoginResponse): void {
    this.accessToken = r.accessToken;
    this._user.set(r.user);
  }

  private clear(): void {
    this.accessToken = null;
    this._user.set(null);
  }
}
