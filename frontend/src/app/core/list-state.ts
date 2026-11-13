import { signal } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';

/**
 * State of a list with search, sorting and pagination done by the server.
 * The page uses it to build the request parameters:
 * `httpResource(() => ({ url, params: list.params() }))`.
 */
export class ListState {
  /** Text in the search field (updated at every key). */
  readonly text = signal('');
  /** Text actually searched (after a short pause). */
  private readonly query = signal('');
  readonly page = signal(0);
  readonly pageSize = signal(20);
  private readonly sort = signal<Sort>({ active: '', direction: '' });
  /** Row highlighted after a save. */
  readonly highlighted = signal<number | null>(null);

  private timer?: ReturnType<typeof setTimeout>;

  /** Searches while typing, without calling the server at every key. */
  search(value: string): void {
    this.text.set(value);
    clearTimeout(this.timer);
    this.timer = setTimeout(() => {
      this.query.set(value.trim());
      this.page.set(0);
    }, 300);
  }

  changePage(e: PageEvent): void {
    this.page.set(e.pageIndex);
    this.pageSize.set(e.pageSize);
  }

  sortBy(s: Sort): void {
    this.sort.set(s);
    this.page.set(0);
  }

  /** Back to the first page (e.g. when a filter changes). */
  firstPage(): void {
    this.page.set(0);
  }

  /** Highlights the item just saved (not the deleted one). */
  highlight(result: { id: number } | 'deleted' | undefined): void {
    if (result && result !== 'deleted') this.highlighted.set(result.id);
  }

  /** page, size, q, sort parameters for the APIs. */
  params(): Record<string, string | number> {
    const params: Record<string, string | number> = { page: this.page(), size: this.pageSize() };
    if (this.query()) params['q'] = this.query();
    const { active, direction } = this.sort();
    if (active && direction) params['sort'] = `${active},${direction}`;
    return params;
  }
}
