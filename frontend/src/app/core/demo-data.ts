import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { CustomersApi } from './customers';

export const DEMO_DATA_API = '/api/demo-data';

export interface DemoDataStatus {
  /** True if the business is still empty and the demo data can be loaded. */
  available: boolean;
}

export interface DemoDataResult {
  customers: number;
  suppliers: number;
  products: number;
  services: number;
}

/** Demo data for an empty business (ADMIN only). */
@Injectable({ providedIn: 'root' })
export class DemoDataApi {
  private readonly http = inject(HttpClient);
  private readonly customers = inject(CustomersApi);

  async load(): Promise<DemoDataResult> {
    const result = await firstValueFrom(this.http.post<DemoDataResult>(DEMO_DATA_API, null));
    // New customers: the birthday notice in the top bar is recalculated.
    this.customers.changes.update((n) => n + 1);
    return result;
  }
}
