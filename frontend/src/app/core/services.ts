import { Injectable } from '@angular/core';

import { ApiResource } from './api-resource';
import { PriceRequest, Prices } from './prices';

/** Service offered by the business (e.g. a treatment). */
export interface ServiceItem extends Prices {
  id: number;
  name: string;
  description: string;
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** Data sent on create and update (version is added on update). */
export interface ServiceItemRequest extends PriceRequest {
  name: string;
  description: string;
  notes: string;
}

export const SERVICES_API = '/api/services';

@Injectable({ providedIn: 'root' })
export class ServicesApi extends ApiResource<ServiceItem, ServiceItemRequest> {
  constructor() {
    super(SERVICES_API);
  }
}
