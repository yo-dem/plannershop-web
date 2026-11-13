import { Component, computed, input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

/**
 * Message box inside the page (error, information, warning).
 * Usage: <app-notice type="error">Text</app-notice>
 */
@Component({
  selector: 'app-notice',
  imports: [MatIcon],
  template: `
    <mat-icon aria-hidden="true">{{ icon() }}</mat-icon>
    <div><ng-content /></div>
  `,
  host: { '[class]': 'type()', role: 'status' },
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
    :host(.error) {
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
    :host(.info) {
      background: var(--mat-sys-secondary-container);
      color: var(--mat-sys-on-secondary-container);
    }
    :host(.warning) {
      background: var(--mat-sys-tertiary-container);
      color: var(--mat-sys-on-tertiary-container);
    }
    div {
      padding-top: 0.125rem;
    }
  `,
})
export class Notice {
  readonly type = input<'error' | 'info' | 'warning'>('info');
  protected readonly icon = computed(() => ({ error: 'error', info: 'info', warning: 'warning' })[this.type()]);
}
