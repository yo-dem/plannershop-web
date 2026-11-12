import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl } from '@angular/forms';

/**
 * Le API rispondono agli errori con un ProblemDetail:
 * { status, detail, errori?: { campo: messaggio } }
 */
export function messaggioErrore(err: unknown, predefinito = 'Si è verificato un errore. Riprova.'): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) return 'Server non raggiungibile. Controlla la connessione.';
    const detail = (err.error as { detail?: string } | null)?.detail;
    if (detail) return detail;
  }
  return predefinito;
}

export function erroriCampo(err: unknown): Record<string, string> {
  if (err instanceof HttpErrorResponse) {
    return (err.error as { errori?: Record<string, string> } | null)?.errori ?? {};
  }
  return {};
}

/**
 * Evidenzia in rosso i campi segnalati dal server.
 *
 * @param trova controllo del modulo per il nome del campo usato dal server
 */
export function segnaCampi(campi: Record<string, string>, trova: (campo: string) => AbstractControl | null): void {
  for (const campo of Object.keys(campi)) {
    const c = trova(campo);
    c?.setErrors({ server: true });
    c?.markAsTouched();
  }
}

/** Messaggio in cima alla finestra quando il salvataggio non riesce. */
export function messaggioSalvataggio(err: unknown, campi: Record<string, string>): string {
  if (Object.keys(campi).length) return 'Controlla i campi evidenziati.';
  if (err instanceof HttpErrorResponse && err.status === 409) {
    return 'Nel frattempo un altro utente ha modificato questi dati. Chiudi e riapri la scheda per vedere i dati aggiornati.';
  }
  return messaggioErrore(err);
}
