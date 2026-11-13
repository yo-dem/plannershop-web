import { Component, Injectable, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';

export interface ConfirmOptions {
  title: string;
  message: string;
  /** Text of the confirm button (e.g. "Delete"). */
  confirm: string;
  /** Red confirm button, for destructive actions. */
  danger?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogModule, MatButton],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>{{ data.message }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton [mat-dialog-close]="false" i18n="@@common.cancel">Cancel</button>
      <button matButton="filled" [mat-dialog-close]="true" [class.danger]="data.danger" cdkFocusInitial>
        {{ data.confirm }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .danger {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class ConfirmDialog {
  protected readonly data = inject<ConfirmOptions>(MAT_DIALOG_DATA);
}

/** Asks for confirmation before an action. Returns true if the user confirms. */
@Injectable({ providedIn: 'root' })
export class Confirm {
  private readonly dialog = inject(MatDialog);

  async ask(options: ConfirmOptions): Promise<boolean> {
    const ref = this.dialog.open(ConfirmDialog, { data: options, width: '26rem', maxWidth: '95vw' });
    return (await firstValueFrom(ref.afterClosed())) === true;
  }
}
