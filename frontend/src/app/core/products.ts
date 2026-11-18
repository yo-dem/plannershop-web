import { Injectable } from '@angular/core';

import { ApiResource } from './api-resource';
import { PriceRequest, Prices } from './prices';
import { Settings } from './settings';

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

export type StockLevel = 'out' | 'low' | 'ok';

/**
 * Colour of the quantity in the products list, with the thresholds chosen by
 * the ADMIN in the settings: at or below the first one red, at or below the
 * second one orange.
 */
export function stockLevel(quantity: number, thresholds: Pick<Settings, 'outOfStockThreshold' | 'lowStockThreshold'>): StockLevel {
  if (quantity <= thresholds.outOfStockThreshold) return 'out';
  return quantity <= thresholds.lowStockThreshold ? 'low' : 'ok';
}

@Injectable({ providedIn: 'root' })
export class ProductsApi extends ApiResource<Product, ProductRequest> {
  constructor() {
    super(PRODUCTS_API);
  }
}
