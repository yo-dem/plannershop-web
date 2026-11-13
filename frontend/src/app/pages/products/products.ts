import { CurrencyPipe, DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatPaginator } from '@angular/material/paginator';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';

import { PageResponse } from '../../core/api-resource';
import { ListState } from '../../core/list-state';
import { PRODUCTS_API, Product, ProductsApi, stockLevel } from '../../core/products';
import { Notice } from '../../core/ui/notice';
import { openProduct } from './product-dialog';

/** Product list with stock highlighted: out of stock in red, running low in orange. */
@Component({
  selector: 'app-products',
  imports: [
    CurrencyPipe, DatePipe, MatButton, MatFormFieldModule, MatIcon, MatInput, MatPaginator, MatProgressBar,
    MatSlideToggle, MatSort, MatSortHeader, MatTableModule, Notice,
  ],
  templateUrl: './products.html',
  styleUrl: './products.scss',
})
export class Products {
  private readonly products = inject(ProductsApi);
  private readonly dialog = inject(MatDialog);
  protected readonly stockLevel = stockLevel;
  protected readonly columns = ['brand', 'quantity', 'salePrice', 'stockDate'];

  protected readonly list = new ListState();
  /** Only products with quantity greater than zero. */
  protected readonly availableOnly = signal(false);

  protected readonly data = httpResource<PageResponse<Product>>(() => {
    this.products.changes();
    const params = this.list.params();
    if (this.availableOnly()) params['available'] = 'true';
    return { url: PRODUCTS_API, params };
  });
  protected readonly items = computed(() => this.data.value()?.items ?? []);
  protected readonly total = computed(() => this.data.value()?.total ?? 0);

  filterAvailable(on: boolean): void {
    this.availableOnly.set(on);
    this.list.firstPage();
  }

  async create(): Promise<void> {
    this.list.highlight(await openProduct(this.dialog, null));
  }

  async open(p: Product): Promise<void> {
    this.list.highlight(await openProduct(this.dialog, p.id));
  }
}
