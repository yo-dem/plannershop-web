import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** Short messages at the bottom of the screen ("Customer saved", errors). */
@Injectable({ providedIn: 'root' })
export class Notifications {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, undefined, { duration: 3000 });
  }

  error(message: string): void {
    this.snackBar.open(message, $localize`:@@common.close:Close`, {
      duration: 8000,
      panelClass: 'error-snackbar',
    });
  }
}
