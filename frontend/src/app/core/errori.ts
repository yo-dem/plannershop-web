import { HttpErrorResponse } from '@angular/common/http';

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
