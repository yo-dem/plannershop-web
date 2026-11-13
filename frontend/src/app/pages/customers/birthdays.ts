import { httpResource } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';

import { CUSTOMERS_API, Customer, CustomersApi } from '../../core/customers';
import { capitalize, monthNames } from '../../core/i18n';
import { Notice } from '../../core/ui/notice';
import { openCustomer } from './customer-dialog';

/** Customers with a birthday in a month (by default the current one). */
@Component({
  selector: 'app-birthdays',
  imports: [
    RouterLink, MatIconButton, MatFormFieldModule, MatIcon, MatProgressBar, MatSelectModule, MatTableModule, Notice,
  ],
  templateUrl: './birthdays.html',
  styleUrl: './customers.scss',
})
export class Birthdays {
  private readonly customers = inject(CustomersApi);
  private readonly dialog = inject(MatDialog);
  protected readonly columns = ['day', 'customer', 'phone', 'email'];

  protected readonly months = monthNames().map((name, i) => ({ name: capitalize(name), value: i + 1 }));
  private readonly today = new Date();
  protected readonly month = signal(this.today.getMonth() + 1);

  protected readonly list = httpResource<Customer[]>(
    () => {
      this.customers.changes();
      return { url: `${CUSTOMERS_API}/birthdays`, params: { month: this.month() } };
    },
    { defaultValue: [] },
  );

  protected isToday(c: Customer): boolean {
    return c.birthMonth === this.today.getMonth() + 1 && c.birthDay === this.today.getDate();
  }

  protected isPast(c: Customer): boolean {
    return c.birthMonth === this.today.getMonth() + 1 && (c.birthDay ?? 0) < this.today.getDate();
  }

  open(c: Customer): void {
    void openCustomer(this.dialog, c.id);
  }
}
