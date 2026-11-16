import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';

import { CustomersApi } from './customers';

export const DEMO_DATA_API = '/api/demo-data';

export interface DemoDataStatus {
  /** True if the business is still empty and the demo data can be loaded. */
  available: boolean;
  /** True if the installation allows resetting the business to the demo data (DEMO_RESET). */
  resettable: boolean;
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

  load(): Promise<DemoDataResult> {
    return this.changed(this.http.post<DemoDataResult>(DEMO_DATA_API, null));
  }

  /** Deletes all the data of the business and loads the demo data. */
  reset(confirmName: string): Promise<DemoDataResult> {
    return this.changed(this.http.post<DemoDataResult>(`${DEMO_DATA_API}/reset`, { confirmName }));
  }

  private async changed(request: Observable<DemoDataResult>): Promise<DemoDataResult> {
    const result = await firstValueFrom(request);
    // New customers: the birthday notice in the top bar is recalculated.
    this.customers.changes.update((n) => n + 1);
    return result;
  }
}
