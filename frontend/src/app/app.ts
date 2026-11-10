import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { Button } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Menu } from 'primeng/menu';
import { Toast } from 'primeng/toast';

import { AuthService } from './core/auth/auth.service';

/** Guscio dell'applicazione: barra superiore (se collegati) + pagina corrente. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Button, Menu, Toast, ConfirmDialog],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly voci = computed(() => {
    const voci = [{ label: 'Home', icon: 'pi pi-home', link: '/' }];
    if (this.auth.haRuolo('SUPERADMIN')) {
      voci.push({ label: 'Attività', icon: 'pi pi-building', link: '/piattaforma' });
    }
    return voci;
  });

  protected readonly menuUtente: MenuItem[] = [
    { label: 'Cambia password', icon: 'pi pi-key', command: () => this.router.navigate(['/cambia-password']) },
    { separator: true },
    { label: 'Esci', icon: 'pi pi-sign-out', command: () => this.auth.logout() },
  ];
}
