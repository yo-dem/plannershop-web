import { DatePipe } from '@angular/common';
import { HttpClient, httpResource } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltip } from '@angular/material/tooltip';
import { firstValueFrom } from 'rxjs';

import { messaggioErrore } from '../../core/errori';
import { Avviso } from '../../core/ui/avviso';
import { Conferma } from '../../core/ui/conferma';
import { Notifiche } from '../../core/ui/notifiche';
import { API_ATTIVITA, AttivitaDialog, AttivitaDto } from './attivita-dialog';

/** Gestione delle attività della piattaforma (solo SUPERADMIN). */
@Component({
  selector: 'app-attivita',
  imports: [DatePipe, MatButton, MatIconButton, MatIcon, MatProgressBar, MatTableModule, MatTooltip, Avviso],
  templateUrl: './attivita.html',
  styleUrl: './attivita.scss',
})
export class Attivita {
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly notifiche = inject(Notifiche);
  private readonly conferma = inject(Conferma);
  protected readonly colonne = ['nome', 'amministratori', 'stato', 'creata', 'azioni'];

  protected readonly elenco = httpResource<AttivitaDto[]>(() => API_ATTIVITA, { defaultValue: [] });

  async apri(attivita: AttivitaDto | null): Promise<void> {
    const ref = this.dialog.open<AttivitaDialog, AttivitaDto | null, boolean>(AttivitaDialog, {
      data: attivita,
      width: '30rem',
      maxWidth: '95vw',
    });
    if (await firstValueFrom(ref.afterClosed())) {
      this.notifiche.successo(attivita ? 'Attività aggiornata' : 'Attività creata');
      this.elenco.reload();
    }
  }

  async elimina(a: AttivitaDto): Promise<void> {
    const ok = await this.conferma.chiedi({
      titolo: "Eliminare l'attività?",
      messaggio: `"${a.nome}" non sarà più accessibile e i suoi utenti verranno scollegati.`,
      conferma: 'Elimina',
      pericolo: true,
    });
    if (!ok) return;
    try {
      await firstValueFrom(this.http.delete(`${API_ATTIVITA}/${a.id}`));
      this.notifiche.successo('Attività eliminata');
      this.elenco.reload();
    } catch (err) {
      this.notifiche.errore(messaggioErrore(err));
    }
  }
}
