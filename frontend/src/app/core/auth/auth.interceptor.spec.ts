import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { LoginResponse } from './auth.models';

const session = (token: string): LoginResponse => ({
  accessToken: token,
  expiresInSeconds: 900,
  tenantChoices: null,
  user: {
    id: 1,
    email: 'a@b.it',
    name: 'Mario',
    role: 'ADMIN',
    superadmin: false,
    tenant: { id: 1, name: 'Shop' },
    mustChangePassword: false,
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

  async function signIn(token: string): Promise<void> {
    const login = auth.login('a@b.it', 'x');
    backend.expectOne('/api/auth/login').flush(session(token));
    await login;
  }

  it('adds the token and the language to /api calls', async () => {
    await signIn('token-1');
    const response = firstValueFrom(http.get('/api/info'));
    const req = backend.expectOne('/api/info');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-1');
    expect(req.request.headers.get('Accept-Language')).toBe('it');
    req.flush({});
    await response;
  });

  it('does not add the token to authentication calls', async () => {
    await signIn('token-1');
    void auth.refresh();
    const req = backend.expectOne('/api/auth/refresh');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush(session('token-2'));
  });

  it('after a 401 renews the session and retries the request', async () => {
    await signIn('expired');
    const response = firstValueFrom(http.get<{ ok: boolean }>('/api/info'));

    backend.expectOne('/api/info').flush(null, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    backend.expectOne('/api/auth/refresh').flush(session('new'));
    await new Promise((r) => setTimeout(r));

    const retried = backend.expectOne('/api/info');
    expect(retried.request.headers.get('Authorization')).toBe('Bearer new');
    retried.flush({ ok: true });
    expect(await response).toEqual({ ok: true });
  });

  it('closes the session if the renewal fails', async () => {
    await signIn('expired');
    const response = firstValueFrom(http.get('/api/info')).catch((e) => e);

    backend.expectOne('/api/info').flush(null, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    backend.expectOne('/api/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });
    await response;

    expect(auth.authenticated()).toBe(false);
  });
});
