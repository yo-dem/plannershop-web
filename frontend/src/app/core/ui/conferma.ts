import { Component, Injectable, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';

export interface OpzioniConferma {
  titolo: string;
  messaggio: string;
  /** Testo del pulsante di conferma (es. "Elimina"). */
  conferma: string;
  /** Pulsante di conferma rosso, per azioni distruttive. */
  pericolo?: boolean;
}

@Component({
  selector: 'app-conferma-dialog',
  imports: [MatDialogModule, MatButton],
  template: `
    <h2 mat-dialog-title>{{ dati.titolo }}</h2>
    <mat-dialog-content>{{ dati.messaggio }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton [mat-dialog-close]="false">Annulla</button>
      <button matButton="filled" [mat-dialog-close]="true" [class.pericolo]="dati.pericolo" cdkFocusInitial>
        {{ dati.conferma }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .pericolo {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class ConfermaDialog {
  protected readonly dati = inject<OpzioniConferma>(MAT_DIALOG_DATA);
}

/** Chiede conferma prima di un'azione. Restituisce true se l'utente conferma. */
@Injectable({ providedIn: 'root' })
export class Conferma {
  private readonly dialog = inject(MatDialog);

  async chiedi(opzioni: OpzioniConferma): Promise<boolean> {
    const ref = this.dialog.open(ConfermaDialog, { data: opzioni, width: '26rem', maxWidth: '95vw' });
    return (await firstValueFrom(ref.afterClosed())) === true;
  }
}
