import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { LoginResponse } from './auth.models';

const sessione = (token: string): LoginResponse => ({
  accessToken: token,
  scadeTraSecondi: 900,
  sceltaTenant: null,
  utente: {
    id: 1,
    email: 'a@b.it',
    nome: 'Mario',
    ruolo: 'ADMIN',
    superadmin: false,
    tenant: { id: 1, nome: 'Negozio' },
    deveCambiarePassword: false,
  },
});

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => backend.verify());

  async function collegati(token: string): Promise<void> {
    const login = auth.login('a@b.it', 'x');
    backend.expectOne('/api/auth/login').flush(sessione(token));
    await login;
  }

  it('aggiunge il token alle chiamate /api', async () => {
    await collegati('token-1');
    const risposta = firstValueFrom(http.get('/api/info'));
    const req = backend.expectOne('/api/info');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-1');
    req.flush({});
    await risposta;
  });

  it('non aggiunge il token alle chiamate di autenticazione', async () => {
    await collegati('token-1');
    void auth.refresh();
    const req = backend.expectOne('/api/auth/refresh');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush(sessione('token-2'));
  });

  it('dopo un 401 rinnova la sessione e ripete la richiesta', async () => {
    await collegati('scaduto');
    const risposta = firstValueFrom(http.get<{ ok: boolean }>('/api/info'));

    backend.expectOne('/api/info').flush(null, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    backend.expectOne('/api/auth/refresh').flush(sessione('nuovo'));
    await new Promise((r) => setTimeout(r));

    const ripetuta = backend.expectOne('/api/info');
    expect(ripetuta.request.headers.get('Authorization')).toBe('Bearer nuovo');
    ripetuta.flush({ ok: true });
    expect(await risposta).toEqual({ ok: true });
  });

  it('se il rinnovo fallisce chiude la sessione', async () => {
    await collegati('scaduto');
    const risposta = firstValueFrom(http.get('/api/info')).catch((e) => e);

    backend.expectOne('/api/info').flush(null, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    backend.expectOne('/api/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });
    await risposta;

    expect(auth.autenticato()).toBe(false);
  });
});
