/** Tipi delle API di autenticazione (vedi AuthDtos.java nel backend). */

export type Ruolo = 'ADMIN' | 'OPERATORE' | 'SUPERADMIN';

export interface TenantOpzione {
  id: number;
  nome: string;
}

export interface UtenteCorrente {
  id: number;
  email: string;
  nome: string;
  ruolo: Ruolo;
  superadmin: boolean;
  tenant: TenantOpzione | null;
  deveCambiarePassword: boolean;
}

export interface LoginResponse {
  accessToken: string | null;
  scadeTraSecondi: number | null;
  utente: UtenteCorrente | null;
  /** Valorizzato se l'utente deve scegliere l'attività prima di entrare. */
  sceltaTenant: TenantOpzione[] | null;
}
