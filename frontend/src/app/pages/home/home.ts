import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { MatButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';

import { AuthService } from '../../core/auth/auth.service';
import { DEMO_DATA_API, DemoDataApi, DemoDataStatus } from '../../core/demo-data';
import { errorMessage } from '../../core/errors';
import { Info } from '../../core/info';
import { Confirm } from '../../core/ui/confirm';
import { Notifications } from '../../core/ui/notifications';

/**
 * Temporary page: checks that the frontend reaches the backend and that the
 * backend reaches the database. The ADMIN of an empty business can load the
 * demo data from here.
 */
@Component({
  selector: 'app-home',
  imports: [DatePipe, MatButton, MatCardModule, MatIcon],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  protected readonly auth = inject(AuthService);
  /** httpResource runs the GET and exposes value(), isLoading(), error() as signals. */
  protected readonly info = httpResource<Info>(() => '/api/info');

  private readonly demoData = inject(DemoDataApi);
  private readonly confirm = inject(Confirm);
  private readonly notifications = inject(Notifications);
  protected readonly demoStatus = httpResource<DemoDataStatus>(() =>
    this.auth.hasRole('ADMIN') ? DEMO_DATA_API : undefined,
  );
  protected readonly loadingDemo = signal(false);

  protected async loadDemoData(): Promise<void> {
    const ok = await this.confirm.ask({
      title: $localize`:@@demo.confirmTitle:Load demo data?`,
      message: $localize`:@@demo.confirmMessage:Fictional customers, suppliers, products and services will be added to the business, to try the application. You can delete them later.`,
      confirm: $localize`:@@demo.confirm:Load`,
    });
    if (!ok) return;
    this.loadingDemo.set(true);
    try {
      const r = await this.demoData.load();
      this.notifications.success(
        $localize`:@@demo.loaded:Loaded ${r.customers}:customers: customers, ${r.suppliers}:suppliers: suppliers, ${r.products}:products: products and ${r.services}:services: services`,
      );
    } catch (err) {
      this.notifications.error(errorMessage(err));
    } finally {
      this.loadingDemo.set(false);
      this.demoStatus.reload();
    }
  }
}
