import { httpResource } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatPaginator } from '@angular/material/paginator';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatSort, MatSortHeader } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';

import { PageResponse } from '../../core/api-resource';
import { ListState } from '../../core/list-state';
import { SUPPLIERS_API, Supplier, SuppliersApi } from '../../core/suppliers';
import { Notice } from '../../core/ui/notice';
import { openSupplier } from './supplier-dialog';

/** Supplier list with search on all fields. */
@Component({
  selector: 'app-suppliers',
  imports: [
    MatButton, MatFormFieldModule, MatIcon, MatInput, MatPaginator, MatProgressBar, MatSort, MatSortHeader,
    MatTableModule, Notice,
  ],
  templateUrl: './suppliers.html',
  styleUrl: './suppliers.scss',
})
export class Suppliers {
  private readonly suppliers = inject(SuppliersApi);
  private readonly dialog = inject(MatDialog);
  protected readonly columns = ['name', 'phone', 'email', 'address'];

  protected readonly list = new ListState();

  protected readonly data = httpResource<PageResponse<Supplier>>(() => {
    this.suppliers.changes();
    return { url: SUPPLIERS_API, params: this.list.params() };
  });
  protected readonly items = computed(() => this.data.value()?.items ?? []);
  protected readonly total = computed(() => this.data.value()?.total ?? 0);

  async create(): Promise<void> {
    this.list.highlight(await openSupplier(this.dialog, null));
  }

  async open(s: Supplier): Promise<void> {
    this.list.highlight(await openSupplier(this.dialog, s.id));
  }
}
