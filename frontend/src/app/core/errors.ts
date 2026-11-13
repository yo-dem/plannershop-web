import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl } from '@angular/forms';

/**
 * The APIs answer errors with a ProblemDetail, already translated into the
 * language of the UI (Accept-Language header):
 * { status, detail, errors?: { field: message } }
 */
export function errorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) return $localize`:@@error.serverUnreachable:Server unreachable. Check the connection.`;
    const detail = (err.error as { detail?: string } | null)?.detail;
    if (detail) return detail;
  }
  return $localize`:@@error.generic:An error occurred. Please try again.`;
}

export function fieldErrors(err: unknown): Record<string, string> {
  if (err instanceof HttpErrorResponse) {
    return (err.error as { errors?: Record<string, string> } | null)?.errors ?? {};
  }
  return {};
}

/**
 * Marks in red the fields reported by the server.
 *
 * @param find form control for the field name used by the server
 */
export function markFields(fields: Record<string, string>, find: (field: string) => AbstractControl | null): void {
  for (const field of Object.keys(fields)) {
    const c = find(field);
    c?.setErrors({ server: true });
    c?.markAsTouched();
  }
}

/** Message at the top of the dialog when saving fails. */
export function saveErrorMessage(err: unknown, fields: Record<string, string>): string {
  if (Object.keys(fields).length) return $localize`:@@error.checkFields:Check the highlighted fields.`;
  if (err instanceof HttpErrorResponse && err.status === 409) {
    return $localize`:@@error.conflict:Meanwhile another user changed this data. Close and reopen the form to see the updated data.`;
  }
  return errorMessage(err);
}
