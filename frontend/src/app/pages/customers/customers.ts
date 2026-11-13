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
import { RouterLink } from '@angular/router';

import { PageResponse } from '../../core/api-resource';
import { CUSTOMERS_API, Customer, CustomersApi, birthday } from '../../core/customers';
import { ListState } from '../../core/list-state';
import { Notice } from '../../core/ui/notice';
import { openCustomer } from './customer-dialog';

/**
 * Customer list: instant search, sorting and pagination done by the server,
 * create and edit in a dialog.
 */
@Component({
  selector: 'app-customers',
  imports: [
    RouterLink, MatButton, MatFormFieldModule, MatIcon, MatInput, MatPaginator, MatProgressBar, MatSort,
    MatSortHeader, MatTableModule, Notice,
  ],
  templateUrl: './customers.html',
  styleUrl: './customers.scss',
})
export class Customers {
  private readonly customers = inject(CustomersApi);
  private readonly dialog = inject(MatDialog);
  protected readonly birthday = birthday;
  protected readonly columns = ['lastName', 'phone', 'email', 'birthday'];

  protected readonly list = new ListState();

  protected readonly data = httpResource<PageResponse<Customer>>(() => {
    this.customers.changes(); // reload after create, update, delete
    return { url: CUSTOMERS_API, params: this.list.params() };
  });
  protected readonly items = computed(() => this.data.value()?.items ?? []);
  protected readonly total = computed(() => this.data.value()?.total ?? 0);

  async create(): Promise<void> {
    this.list.highlight(await openCustomer(this.dialog, null));
  }

  async open(c: Customer): Promise<void> {
    this.list.highlight(await openCustomer(this.dialog, c.id));
  }
}
