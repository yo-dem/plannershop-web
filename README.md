# PlannerShop Web

Gestionale multi-tenant per attività commerciali: clienti, catalogo, vendite, agenda, statistiche.
Riscrittura web dell'applicazione desktop PlannerShop (WinForms).

| Parte | Tecnologia | Cartella |
|---|---|---|
| Backend | Java 21, Spring Boot 4, PostgreSQL 18, Flyway | `backend/` |
| Frontend | Angular 22, Angular Material (Material 3) | `frontend/` |
| Deploy | Docker Compose (postgres, backend, nginx) | `docker/` |

Specifica funzionale e tecnica: [docs/SPECIFICA.md](docs/SPECIFICA.md)

## Stato attuale

Funzionano: login con scelta dell'attività, sessione con rinnovo automatico, logout, cambio
password (obbligatorio se provvisoria), ruoli SUPERADMIN / ADMIN / OPERATORE, gestione delle
attività da parte del SUPERADMIN, modalità di installazione `cloud` e `locale`, ripristino
password da riga di comando, **clienti** (elenco con ricerca, ordinamento e paginazione,
inserimento, modifica, eliminazione solo per l'ADMIN, compleanni del mese con avviso in alto),
**fornitori** (IBAN verificato), **prodotti** (giacenza evidenziata, filtro "solo disponibili")
e **servizi**, con calcolo del prezzo netto o IVA inclusa.

Non ancora presenti: vendite, agenda, impostazioni, email,
statistiche, migrazione dei dati dal vecchio programma (piano in `docs/SPECIFICA.md` §13).

## Requisiti

- **Docker Desktop**, in esecuzione (database, test, stack completo)
- **JDK 21** (Eclipse Temurin), ad esempio scaricato da IntelliJ IDEA
  (*File → Project Structure → SDKs → Download JDK*)
- **Node.js** tramite nvm: la versione è in `frontend/.nvmrc` (`nvm install` la prima volta)
- **Angular CLI** (`npm install -g @angular/cli`), oppure usa `npm start` al posto di `ng serve`

Maven non va installato: si usa il wrapper `./mvnw`.

## Avvio in sviluppo

Tre processi, ciascuno nel proprio terminale:

```bash
# 1. Database → localhost:5432 (una volta, poi resta acceso)
cd docker && docker compose up -d postgres

# 2. Backend → http://localhost:8080
#    da IntelliJ: ▶ su PlannershopApplication (Run o Debug)
#    oppure da terminale:
cd backend && ./mvnw spring-boot:run

# 3. Frontend → http://localhost:4200  ← apri questo nel browser
cd frontend && nvm use && npm install && ng serve
```

Durante `ng serve` le chiamate `/api` vengono inoltrate al backend sulla 8080 (`proxy.conf.json`).

**Consigliato:** imposta un segreto JWT fisso, altrimenti a ogni riavvio del backend devi rifare
il login. In IntelliJ: *Run → Edit Configurations → PlannershopApplication → Environment variables*

```
JWT_SECRET=un-segreto-di-sviluppo-lungo-almeno-32-caratteri
```

### Primo accesso

- **Database nuovo:** al primo avvio il backend crea il SUPERADMIN `admin@plannershop.local`
  con una password casuale scritta nel log (cerca *"Creato il SUPERADMIN"*), da cambiare al primo
  accesso. Dal menu *Attività* il SUPERADMIN crea le attività e i loro amministratori.
- **Utenti di sviluppo già creati** (super amministratore, amministratore, operatore): vedi
  `docs/CREDENZIALI-SVILUPPO.md`. Il file è solo locale, escluso da git.

### Indirizzi utili

| Cosa | Indirizzo |
|---|---|
| Applicazione (sviluppo) | http://localhost:4200 |
| Stato del backend | http://localhost:8080/actuator/health → `{"status":"UP"}` |
| Documentazione API (Swagger) | http://localhost:8080/api/swagger-ui.html |
| Database | `localhost:5432`, db/utente/password `plannershop` (solo sviluppo) |

`http://localhost:8080` da solo mostra una pagina bianca: è normale, il backend espone
solo API e senza login risponde 401.

## Test

```bash
cd backend && ./mvnw verify            # Testcontainers: Docker deve essere acceso
cd frontend && ng test --watch=false
```

Da IntelliJ: clic destro su `backend/src/test/java` → *Run 'All Tests'*.

## Stack completo nei container

Come in produzione: nginx serve il frontend e inoltra `/api` al backend.

```bash
cd docker
cp .env.example .env                    # modifica password e JWT_SECRET (openssl rand -base64 48)
docker compose up -d --build            # → http://localhost:8090
docker compose logs backend | grep -A3 SUPERADMIN   # password iniziale (primo avvio)
docker compose logs -f backend          # log del backend
docker compose down                     # ferma (i dati restano nel volume pgdata)
docker compose down -v                  # ferma e CANCELLA il database
```

Lo stack completo usa lo stesso database dello sviluppo. Il backend nel container non occupa
la porta 8080 del PC, quindi può restare acceso insieme a quello avviato da IntelliJ.

### Modalità di installazione

La variabile `APP_MODE` (nel `.env`, oppure nelle Environment variables di IntelliJ) va scelta
**prima del primo avvio**, su un database vuoto: decide quali utenti vengono creati.

| `APP_MODE` | Uso | Al primo avvio crea |
|---|---|---|
| `cloud` (default) | Piattaforma con più attività | il SUPERADMIN (`SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD`) |
| `locale` | PC di un'unica attività | l'attività e il suo ADMIN; SUPERADMIN e gestione attività non esistono |

Esempio di `.env` per un'installazione locale:

```bash
APP_MODE=locale
ACTIVITY_NAME=Nome del negozio
ADMIN_EMAIL=titolare@esempio.it
ADMIN_NAME=Nome Cognome
ADMIN_PASSWORD=                         # vuota = generata e scritta nel log
```

```bash
docker compose logs backend | grep -A4 "Installazione locale"   # password iniziale
```

La password iniziale è sempre provvisoria. Dettagli in `docs/SPECIFICA.md` §4.1.

### Password dimenticata

```bash
cd docker && docker compose run --rm backend --app.reset-password=email@utente
```

Stampa una nuova password provvisoria e chiude le sessioni aperte dell'utente. Funziona anche
con l'applicazione avviata. Da IntelliJ: aggiungi `--app.reset-password=email@utente` in
*Program arguments* di una copia della run configuration.

## Problemi comuni

| Sintomo | Causa e soluzione |
|---|---|
| Pagina bianca su `localhost:8080` | Normale: è solo l'API. Apri `localhost:4200` (sviluppo) o `localhost:8090` (container). |
| "Email o password non corretti" | Password cambiata o database ricreato: usa il ripristino password. Dopo 10 tentativi falliti il login si blocca per 15 minuti (oppure riavvia il backend). |
| Scollegato dopo ogni riavvio del backend | Manca `JWT_SECRET`: impostalo come sopra. |
| Il backend non parte: errore di connessione al database | Il container `postgres` non è acceso: `docker compose up -d postgres`. |
| `Port 8080 was already in use` | C'è già un backend avviato (IntelliJ o terminale): fermalo. |
| `docker: command not found` | Docker Desktop non è avviato, oppure manca `~/.docker/bin` nel PATH. |
| I test del backend falliscono subito | Docker non è acceso (servono per Testcontainers). |

## Struttura

```
backend/
  src/main/java/it/plannershop/
    common/          classi base (BaseEntity, TenantScopedEntity), errori API, impostazioni app,
                     calcolo prezzi, verifica IBAN, ricerca e ordinamento degli elenchi
    tenant/          attività e risoluzione del tenant corrente (@TenantId)
    security/        Spring Security, JWT, limite tentativi di login
    user/            utenti, ruoli per attività, ripristino password
    auth/            login, refresh, logout, cambio password, /api/me
    cliente/         anagrafica clienti e compleanni
    fornitore/       fornitori
    prodotto/        prodotti e magazzino
    servizio/        servizi offerti
    platform/        gestione attività (SUPERADMIN) e sua creazione — modalità cloud
    locale/          creazione dell'attività al primo avvio — modalità locale
    info/            /api/info (versione, database, modalità)
  src/main/resources/
    application.yml  configurazione (variabili d'ambiente)
    db/migration/    migrazioni Flyway V<n>__descrizione.sql
  src/test/          test d'integrazione (Testcontainers)
frontend/
  src/app/core/      sessione, interceptor del token, guard, servizi API, prezzi, stato elenchi
  src/app/core/ui/   componenti comuni: avvisi, notifiche, conferme, paginatore, campi prezzo
  src/app/pages/     pagine (login, home, cambia password, clienti, fornitori, prodotti, servizi, piattaforma)
  proxy.conf.json    /api → localhost:8080 durante ng serve
  nginx.conf         configurazione nginx dell'immagine Docker
docker/
  docker-compose.yml, .env.example
docs/
  SPECIFICA.md
  CREDENZIALI-*.md   credenziali di sviluppo (solo locali, escluse da git)
```
