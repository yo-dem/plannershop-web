import { Injectable } from '@angular/core';

import { ApiResource } from './api-resource';
import { PriceRequest, Prices } from './prices';

export interface Product extends Prices {
  id: number;
  /** "2026-03-15" */
  stockDate: string | null;
  brand: string;
  description: string;
  quantity: number;
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** Data sent on create and update (version is added on update). */
export interface ProductRequest extends PriceRequest {
  stockDate: string | null;
  brand: string;
  description: string;
  quantity: number;
  notes: string;
}

export const PRODUCTS_API = '/api/products';

/**
 * At or below this quantity the product is "running low".
 * It will become a tenant setting (SPECIFICA §6.9, soglia_scorta_bassa).
 */
export const LOW_STOCK_THRESHOLD = 10;

export type StockLevel = 'out' | 'low' | 'ok';

export function stockLevel(quantity: number, threshold = LOW_STOCK_THRESHOLD): StockLevel {
  if (quantity <= 0) return 'out';
  return quantity <= threshold ? 'low' : 'ok';
}

@Injectable({ providedIn: 'root' })
export class ProductsApi extends ApiResource<Product, ProductRequest> {
  constructor() {
    super(PRODUCTS_API);
  }
}
