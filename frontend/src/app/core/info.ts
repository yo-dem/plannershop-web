/** Response of GET /api/info (see InfoController in the backend). */
export interface Info {
  application: string;
  version: string;
  database: string;
  /** CLOUD = platform with many tenants; LOCAL = installed on a single PC. */
  mode: 'CLOUD' | 'LOCAL';
  serverTime: string;
}
