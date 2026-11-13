import { CurrencyPipe } from '@angular/common';
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
import { SERVICES_API, ServiceItem, ServicesApi } from '../../core/services';
import { Notice } from '../../core/ui/notice';
import { openService } from './service-dialog';

/** List of the services offered (treatments, appointments). */
@Component({
  selector: 'app-services',
  imports: [
    CurrencyPipe, MatButton, MatFormFieldModule, MatIcon, MatInput, MatPaginator, MatProgressBar, MatSort,
    MatSortHeader, MatTableModule, Notice,
  ],
  templateUrl: './services.html',
  styleUrl: '../products/products.scss',
})
export class Services {
  private readonly services = inject(ServicesApi);
  private readonly dialog = inject(MatDialog);
  protected readonly columns = ['name', 'description', 'salePrice'];

  protected readonly list = new ListState();

  protected readonly data = httpResource<PageResponse<ServiceItem>>(() => {
    this.services.changes();
    return { url: SERVICES_API, params: this.list.params() };
  });
  protected readonly items = computed(() => this.data.value()?.items ?? []);
  protected readonly total = computed(() => this.data.value()?.total ?? 0);

  async create(): Promise<void> {
    this.list.highlight(await openService(this.dialog, null));
  }

  async open(s: ServiceItem): Promise<void> {
    this.list.highlight(await openService(this.dialog, s.id));
  }
}
