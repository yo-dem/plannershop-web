import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';

import { DemoDataApi, DemoDataResult } from '../../core/demo-data';
import { errorMessage } from '../../core/errors';
import { Notice } from '../../core/ui/notice';

interface ResetDemoData {
  tenantName: string;
}

/**
 * Confirmation of the reset to the demo data: the user must type the name of
 * the business, as GitHub does before deleting a repository.
 */
@Component({
  selector: 'app-reset-demo-dialog',
  imports: [FormsModule, MatButton, MatDialogModule, MatFormFieldModule, MatInput, Notice],
  template: `
    <h2 mat-dialog-title i18n="@@demo.resetTitle">Reset to the demo data?</h2>
    <mat-dialog-content>
      @if (error(); as e) {
        <app-notice type="error">{{ e }}</app-notice>
      }
      <p i18n="@@demo.resetWarning">
        <strong>All</strong> the customers, suppliers, products and services of the business will be permanently
        deleted and replaced by the demo data. Users are not changed. This cannot be undone.
      </p>
      <p i18n="@@demo.resetTypeName">To confirm, type the name of the business: <strong>{{ data.tenantName // i18n(ph="name") }}</strong></p>
      <mat-form-field class="name">
        <mat-label i18n="@@demo.resetNameLabel">Name of the business</mat-label>
        <input matInput [(ngModel)]="typed" autocomplete="off" (keydown.enter)="matches() && reset()" />
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton mat-dialog-close i18n="@@common.cancel">Cancel</button>
      <button matButton="filled" class="danger" [disabled]="!matches() || busy()" (click)="reset()" i18n="@@demo.resetConfirm">
        Delete and reset
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .name {
      width: 100%;
    }
    .danger:not([disabled]) {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class ResetDemoDialog {
  protected readonly data = inject<ResetDemoData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<ResetDemoDialog, DemoDataResult>>(MatDialogRef);
  private readonly api = inject(DemoDataApi);

  protected typed = '';
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Same rule as the backend: spaces and case do not matter. */
  protected matches(): boolean {
    return this.typed.trim().toLowerCase() === this.data.tenantName.trim().toLowerCase();
  }

  protected async reset(): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      this.ref.close(await this.api.reset(this.typed));
    } catch (err) {
      this.error.set(errorMessage(err));
      this.busy.set(false);
    }
  }
}

/** Opens the confirmation; the result is undefined if the user cancels. */
export async function openResetDemo(dialog: MatDialog, tenantName: string): Promise<DemoDataResult | undefined> {
  const ref = dialog.open<ResetDemoDialog, ResetDemoData, DemoDataResult>(ResetDemoDialog, {
    data: { tenantName },
    width: '30rem',
    maxWidth: '95vw',
  });
  return firstValueFrom(ref.afterClosed());
}
