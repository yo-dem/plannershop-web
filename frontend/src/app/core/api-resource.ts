import { HttpClient } from '@angular/common/http';
import { inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

/** Paged API response (see PageResponse in the backend). */
export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

/**
 * CRUD calls to an API resource (customers, products, services...).
 * Subclasses only give the address, e.g. `super('/api/products')`.
 */
export abstract class ApiResource<T, R> {
  private readonly http = inject(HttpClient);

  /**
   * Grows at every create, update or delete: whoever shows this data (lists,
   * notices) reads it to reload.
   */
  readonly changes = signal(0);

  protected constructor(readonly url: string) {}

  get(id: number): Promise<T> {
    return firstValueFrom(this.http.get<T>(`${this.url}/${id}`));
  }

  async create(data: R): Promise<T> {
    const r = await firstValueFrom(this.http.post<T>(this.url, data));
    this.changed();
    return r;
  }

  async update(id: number, data: R & { version: number }): Promise<T> {
    const r = await firstValueFrom(this.http.put<T>(`${this.url}/${id}`, data));
    this.changed();
    return r;
  }

  async delete(id: number): Promise<void> {
    await firstValueFrom(this.http.delete(`${this.url}/${id}`));
    this.changed();
  }

  private changed(): void {
    this.changes.update((n) => n + 1);
  }
}
