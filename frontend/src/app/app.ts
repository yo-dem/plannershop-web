import { httpResource } from '@angular/common/http';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from './core/auth/auth.service';
import { CUSTOMERS_API, Customer, CustomersApi } from './core/customers';

interface NavItem {
  label: string;
  icon: string;
  link: string;
}

/** Width below which the side menu is hidden and opened with the ☰ button. */
const HANDSET_QUERY = '(max-width: 840px)';

/** Application shell: top bar and side menu (when signed in) + current page. */
@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButton,
    MatIconButton,
    MatIcon,
    MatListModule,
    MatMenuModule,
    MatSidenavModule,
    MatTooltip,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  private readonly customers = inject(CustomersApi);

  /** True on smartphones and narrow windows: the side menu floats over the page. */
  private readonly breakpoints = inject(BreakpointObserver);
  protected readonly handset = toSignal(
    this.breakpoints.observe(HANDSET_QUERY).pipe(map((state) => state.matches)),
    { initialValue: this.breakpoints.isMatched(HANDSET_QUERY) },
  );

  protected readonly navItems = computed(() => {
    const items: NavItem[] = [{ label: $localize`:@@nav.home:Home`, icon: 'home', link: '/' }];
    if (this.auth.hasRole('ADMIN', 'OPERATOR')) {
      items.push(
        { label: $localize`:@@nav.customers:Customers`, icon: 'group', link: '/customers' },
        { label: $localize`:@@nav.suppliers:Suppliers`, icon: 'local_shipping', link: '/suppliers' },
        { label: $localize`:@@nav.products:Products`, icon: 'inventory_2', link: '/products' },
        { label: $localize`:@@nav.services:Services`, icon: 'content_cut', link: '/services' },
      );
    }
    if (this.auth.hasRole('SUPERADMIN')) {
      items.push({ label: $localize`:@@nav.tenants:Businesses`, icon: 'store', link: '/platform' });
    }
    return items;
  });

  /** Birthdays of the current month (only for tenant users), for the notice in the top bar. */
  protected readonly birthdays = httpResource<Customer[]>(() => {
    this.customers.changes();
    return this.auth.hasRole('ADMIN', 'OPERATOR') ? `${CUSTOMERS_API}/birthdays` : undefined;
  });
  protected readonly birthdayCount = computed(() => this.birthdays.value()?.length ?? 0);
  protected readonly birthdayText = computed(() => {
    const n = this.birthdayCount();
    return n === 1
      ? $localize`:@@nav.birthdays.one:1 birthday this month`
      : $localize`:@@nav.birthdays.other:${n}:count: birthdays this month`;
  });
}
