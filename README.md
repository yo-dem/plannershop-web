# PlannerShop Web

Gestionale multi-tenant per attività commerciali: clienti, catalogo, vendite, agenda, statistiche.

| Parte | Tecnologia | Cartella |
|---|---|---|
| Backend | Java 21, Spring Boot 4, PostgreSQL 18, Flyway | `backend/` |
| Frontend | Angular 22, PrimeNG (tema Aura) | `frontend/` |
| Deploy | Docker Compose (postgres, backend, nginx) | `docker/` |

Specifica funzionale e tecnica: [docs/SPECIFICA.md](docs/SPECIFICA.md)

## Requisiti

- Docker Desktop (in esecuzione)
- JDK 21 (Eclipse Temurin), ad esempio scaricato da IntelliJ IDEA
- Node.js LTS tramite nvm (`cd frontend && nvm use`), Angular CLI

Maven non va installato: si usa il wrapper `./mvnw`.

## Sviluppo

```bash
# 1. Database (una volta, poi resta acceso)
cd docker && docker compose up -d postgres

# 2. Backend → http://localhost:8080
#    da IntelliJ: avvia PlannershopApplication
#    oppure da terminale:
cd backend && ./mvnw spring-boot:run

# 3. Frontend → http://localhost:4200 (le chiamate /api vanno al backend)
cd frontend && npm install && ng serve
```

- **Primo accesso**: al primo avvio il backend crea il SUPERADMIN `admin@plannershop.local`
  con una password casuale scritta nel log (cerca "Creato il SUPERADMIN"); va cambiata al primo accesso.
  Dalla pagina *Attività* il SUPERADMIN crea le attività e i loro amministratori.
- Documentazione API (Swagger UI): http://localhost:8080/api/swagger-ui.html
- Database: `localhost:5432`, db/utente/password `plannershop` (solo sviluppo)

## Test

```bash
cd backend && ./mvnw verify       # usa Testcontainers: Docker deve essere acceso
cd frontend && ng test --watch=false
```

## Stack completo nei container

```bash
cd docker
cp .env.example .env              # e modifica password e JWT_SECRET
docker compose up -d --build      # → http://localhost:8090
docker compose logs backend | grep -A3 SUPERADMIN   # password iniziale (primo avvio)
docker compose down               # ferma (i dati restano nel volume pgdata)
```

### Installazione locale (PC di un'unica attività)

Nel file `.env`, **prima del primo avvio**:

```bash
APP_MODALITA=locale
ATTIVITA_NOME=Nome del negozio
ADMIN_EMAIL=titolare@esempio.it
ADMIN_PASSWORD=                   # vuota = generata e scritta nel log
```

Al primo avvio vengono creati l'attività e il suo amministratore (password provvisoria);
il SUPERADMIN e la gestione attività non esistono. Vedi `docs/SPECIFICA.md` §4.1.

```bash
docker compose logs backend | grep -A4 "Installazione locale"   # password iniziale
```

### Password dimenticata

```bash
docker compose run --rm backend --app.reset-password=email@utente
```

Stampa una nuova password provvisoria (da cambiare al primo accesso) e chiude le sessioni
aperte dell'utente. Funziona anche con l'applicazione avviata. Da IntelliJ: aggiungi
`--app.reset-password=email@utente` in *Program arguments* di una copia della run configuration.

## Struttura

```
backend/
  src/main/java/it/plannershop/
    common/                       classi base (BaseEntity, TenantScopedEntity), errori API
    tenant/                       attività e risoluzione del tenant corrente (@TenantId)
    security/                     Spring Security, JWT, limite tentativi di login
    user/                         utenti e ruoli per attività
    auth/                         login, refresh, logout, /api/me
    platform/                     gestione attività (SUPERADMIN), creazione SUPERADMIN
  src/main/resources/
    application.yml               configurazione (variabili d'ambiente)
    db/migration/                 migrazioni Flyway V<n>__descrizione.sql
frontend/
  src/app/core/auth/              sessione, interceptor del token, guard
  src/app/pages/                  pagine (login, home, cambia password, attività)
  proxy.conf.json                 /api → localhost:8080 durante ng serve
  nginx.conf                      configurazione nginx dell'immagine Docker
docker/
  docker-compose.yml, .env.example
docs/
  SPECIFICA.md
```
