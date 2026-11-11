/** Risposta di GET /api/info (vedi InfoController nel backend). */
export interface Info {
  applicazione: string;
  versione: string;
  database: string;
  /** CLOUD = piattaforma con più attività; LOCALE = installazione su un solo PC. */
  modalita: 'CLOUD' | 'LOCALE';
  oraServer: string;
}
