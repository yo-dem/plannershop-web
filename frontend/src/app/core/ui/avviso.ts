import { Component, computed, input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

/**
 * Riquadro di messaggio dentro la pagina (errore, informazione, attenzione).
 * Uso: <app-avviso tipo="errore">Testo</app-avviso>
 */
@Component({
  selector: 'app-avviso',
  imports: [MatIcon],
  template: `
    <mat-icon aria-hidden="true">{{ icona() }}</mat-icon>
    <div><ng-content /></div>
  `,
  host: { '[class]': 'tipo()', role: 'status' },
  styles: `
    :host {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
      margin-bottom: 1rem;
      padding: 0.75rem 1rem;
      border-radius: var(--mat-sys-corner-medium);
      font: var(--mat-sys-body-medium);
    }
    :host(.errore) {
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
    :host(.info) {
      background: var(--mat-sys-secondary-container);
      color: var(--mat-sys-on-secondary-container);
    }
    :host(.attenzione) {
      background: var(--mat-sys-tertiary-container);
      color: var(--mat-sys-on-tertiary-container);
    }
    div {
      padding-top: 0.125rem;
    }
  `,
})
export class Avviso {
  readonly tipo = input<'errore' | 'info' | 'attenzione'>('info');
  protected readonly icona = computed(
    () => ({ errore: 'error', info: 'info', attenzione: 'warning' })[this.tipo()],
  );
}
