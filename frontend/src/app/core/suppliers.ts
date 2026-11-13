import { Injectable } from '@angular/core';

import { ApiResource } from './api-resource';

export interface Supplier {
  id: number;
  name: string;
  address: string;
  phone: string;
  email: string | null;
  /** No spaces, upper case. */
  iban: string | null;
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** Data sent on create and update (version is added on update). */
export interface SupplierRequest {
  name: string;
  address: string;
  phone: string;
  email: string;
  iban: string;
  notes: string;
}

export const SUPPLIERS_API = '/api/suppliers';

@Injectable({ providedIn: 'root' })
export class SuppliersApi extends ApiResource<Supplier, SupplierRequest> {
  constructor() {
    super(SUPPLIERS_API);
  }
}
