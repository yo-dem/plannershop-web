import { httpResource } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth/auth.service';
import { API_CLIENTI, Cliente, ClientiService } from './core/clienti';

/** Guscio dell'applicazione: barra superiore (se collegati) + pagina corrente. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButton, MatIcon, MatMenuModule, MatTooltip],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  private readonly clienti = inject(ClientiService);

  protected readonly voci = computed(() => {
    const voci = [{ label: 'Home', icon: 'home', link: '/' }];
    if (this.auth.haRuolo('ADMIN', 'OPERATORE')) {
      voci.push({ label: 'Clienti', icon: 'group', link: '/clienti' });
    }
    if (this.auth.haRuolo('SUPERADMIN')) {
      voci.push({ label: 'Attività', icon: 'store', link: '/piattaforma' });
    }
    return voci;
  });

  /** Compleanni del mese corrente (solo per chi lavora in un'attività), per l'avviso in alto. */
  protected readonly compleanni = httpResource<Cliente[]>(() => {
    this.clienti.modifiche();
    return this.auth.haRuolo('ADMIN', 'OPERATORE') ? `${API_CLIENTI}/compleanni` : undefined;
  });
  protected readonly numeroCompleanni = computed(() => this.compleanni.value()?.length ?? 0);
}
