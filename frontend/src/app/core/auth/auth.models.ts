/** Types of the authentication APIs (see AuthDtos.java in the backend). */

export type Role = 'ADMIN' | 'OPERATOR' | 'SUPERADMIN';

export interface TenantOption {
  id: number;
  name: string;
}

export interface CurrentUser {
  id: number;
  email: string;
  name: string;
  role: Role;
  superadmin: boolean;
  tenant: TenantOption | null;
  mustChangePassword: boolean;
}

export interface LoginResponse {
  accessToken: string | null;
  expiresInSeconds: number | null;
  user: CurrentUser | null;
  /** Set when the user must choose the tenant before signing in. */
  tenantChoices: TenantOption[] | null;
}
