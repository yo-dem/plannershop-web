import { HttpClient, httpResource } from '@angular/common/http';
import { Injectable, computed, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AuthService } from './auth/auth.service';

export const SETTINGS_API = '/api/settings';

/** Settings of the business (SPECIFICA §6.9). */
export interface Settings {
  /** Products with quantity at or below this value are shown in red. */
  outOfStockThreshold: number;
  /** Products with quantity at or below this value (and above the red one) are shown in orange. */
  lowStockThreshold: number;
  /** Null until the business saves its settings for the first time. */
  version: number | null;
}

export type SettingsRequest = Omit<Settings, 'version'> & { version: number | null };

/** Values used until the settings are loaded; the same defaults as the backend. */
export const DEFAULT_SETTINGS: Settings = { outOfStockThreshold: 0, lowStockThreshold: 10, version: null };

/**
 * Settings of the current business, loaded once after sign-in and shared by
 * all the pages. Everyone in the business reads them, only the ADMIN saves.
 */
@Injectable({ providedIn: 'root' })
export class SettingsApi {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private readonly resource = httpResource<Settings>(() =>
    this.auth.hasRole('ADMIN', 'OPERATOR') ? SETTINGS_API : undefined,
  );
  /** Current settings (the defaults while loading or if loading fails). */
  readonly value = computed(() => this.resource.value() ?? DEFAULT_SETTINGS);
  readonly isLoading = this.resource.isLoading;
  readonly error = this.resource.error;

  async save(request: SettingsRequest): Promise<Settings> {
    const saved = await firstValueFrom(this.http.put<Settings>(SETTINGS_API, request));
    this.resource.set(saved);
    return saved;
  }

  reload(): void {
    this.resource.reload();
  }
}
