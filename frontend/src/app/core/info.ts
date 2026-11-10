/** Risposta di GET /api/info (vedi InfoController nel backend). */
export interface Info {
  applicazione: string;
  versione: string;
  database: string;
  oraServer: string;
}
