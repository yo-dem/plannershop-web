import { Injectable } from '@angular/core';

import { ApiResource } from './api-resource';
import { monthNames } from './i18n';

export interface Customer {
  id: number;
  firstName: string;
  lastName: string;
  birthDay: number | null;
  birthMonth: number | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** Data sent on create and update (version is added on update). */
export interface CustomerRequest {
  firstName: string;
  lastName: string;
  birthDay: number | null;
  birthMonth: number | null;
  phone: string;
  email: string;
  address: string;
  notes: string;
}

export const CUSTOMERS_API = '/api/customers';

/** "29 febbraio" (day and month name in the UI language), or empty if the birthday is missing. */
export function birthday(c: Pick<Customer, 'birthDay' | 'birthMonth'>, months = monthNames()): string {
  return c.birthDay && c.birthMonth ? `${c.birthDay} ${months[c.birthMonth - 1]}` : '';
}

/** Calls to the customer APIs, shared by the pages. */
@Injectable({ providedIn: 'root' })
export class CustomersApi extends ApiResource<Customer, CustomerRequest> {
  constructor() {
    super(CUSTOMERS_API);
  }
}
