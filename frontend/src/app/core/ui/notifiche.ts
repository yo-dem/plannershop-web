import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** Brevi messaggi in basso allo schermo ("Cliente salvato", errori). */
@Injectable({ providedIn: 'root' })
export class Notifiche {
  private readonly snackBar = inject(MatSnackBar);

  successo(messaggio: string): void {
    this.snackBar.open(messaggio, undefined, { duration: 3000 });
  }

  errore(messaggio: string): void {
    this.snackBar.open(messaggio, 'Chiudi', { duration: 8000, panelClass: 'notifica-errore' });
  }
}
