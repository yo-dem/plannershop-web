# PlannerShop Web — Specifica funzionale e tecnica

> Versione 0.5 — 12/11/2026
> Fonte: analisi del progetto desktop `PlannerShop` (C# WinForms, .NET 8, SQLite).
> Scopo: riscrivere l'applicativo come web app multi-tenant con backend Java/Spring Boot e frontend Angular, containerizzabile e ospitabile online.

---

## 1. Obiettivi

1. Funzionalità equivalenti all'applicativo desktop attuale (clienti, prodotti, servizi, fornitori, vendite, agenda, statistiche, email, impostazioni).
2. **Multi-tenant**: una sola installazione serve più attività (inizialmente una), con dati rigorosamente separati.
3. **Ruoli**: Amministratore e Operatore per ogni attività, più un Superamministratore di piattaforma.
4. **API REST** riutilizzabile da un futuro client mobile (PWA o app Capacitor).
5. Esecuzione locale via Docker Compose; stessa configurazione utilizzabile su un server online.

## 2. Stack tecnologico

| Livello | Scelta |
|---|---|
| Backend | Java 21, Spring Boot 4, Maven (wrapper) |
| Persistenza | Spring Data JPA (Hibernate 7), PostgreSQL 18, Flyway |
| Sicurezza | Spring Security, `oauth2-resource-server` con JWT firmati dal backend, refresh token in cookie `httpOnly` |
| API docs | springdoc-openapi → generazione automatica del client TypeScript |
| Email | Spring Boot Mail (SMTP per tenant) |
| Frontend | Angular 22, TypeScript, Angular Material 22 (Material 3, tema verde), icone Material Symbols e carattere Roboto inclusi nel progetto (funzionano offline) |
| Agenda | FullCalendar (`@fullcalendar/angular`), solo plugin MIT (i plugin "premium" sono a pagamento) |
| Grafici | Chart.js (MIT) |

**Licenze**: si usano solo librerie open source (MIT, Apache 2.0, OFL). PrimeNG è stato scartato perché dalla versione 22 richiede una licenza commerciale PrimeUI (§14).
| Deploy | Docker Compose: `postgres`, `backend`, `frontend` (nginx + reverse proxy `/api`) |

## 3. Struttura del repository

```
plannershop-web/
├── backend/     Spring Boot
├── frontend/    Angular (+ Dockerfile e nginx.conf)
├── docker/      docker-compose.yml, .env.example
└── docs/        specifica e decisioni architetturali
```

---

## 4. Multi-tenancy

- **Strategia**: schema condiviso, colonna `tenant_id` su ogni tabella di dominio.
- Le entità di dominio hanno un campo annotato `@TenantId` (Hibernate 7): il filtro `tenant_id = ?` è applicato automaticamente a ogni query e il valore è impostato automaticamente in inserimento.
- Il tenant corrente è letto **esclusivamente** dal token dell'utente autenticato (claim `tenant`), tramite un `CurrentTenantIdentifierResolver`. Il client non invia mai il tenant come parametro.
- Unicità e indici sono sempre composti con `tenant_id` (es. `UNIQUE (tenant_id, email)`).
- Evoluzione possibile: Row Level Security di PostgreSQL come seconda barriera.

### 4.1 Modalità di installazione

Variabile `APP_MODE`, da scegliere prima del primo avvio. Il codice e lo schema del database sono gli stessi nelle due modalità: un'installazione locale può in futuro passare online senza conversioni.

| | `cloud` (default) | `locale` |
|---|---|---|
| Uso | Piattaforma online con più attività | PC di un'unica attività |
| Primo avvio (nessun utente) | Crea il SUPERADMIN (`SUPERADMIN_*`) | Crea l'attività e il suo ADMIN (`ACTIVITY_NAME`, `ADMIN_*`), password sempre provvisoria |
| SUPERADMIN e `/api/platform/**` | Attivi | Assenti: le API di piattaforma non esistono e il ruolo SUPERADMIN non viene mai inserito nei token; un utente superadmin senza attività non può accedere |
| `GET /api/info` | `modalita: CLOUD` | `modalita: LOCALE` |

**Ripristino password** (entrambe le modalità, indispensabile in locale dove non c'è un SUPERADMIN): avviando il backend con `--app.reset-password=<email>` si assegna all'utente una password provvisoria (stampata nel log), lo si riattiva e se ne chiudono le sessioni; poi il processo termina (il server web usa una porta casuale, quindi non va in conflitto con l'istanza già avviata). Con Docker: `docker compose run --rm backend --app.reset-password=<email>`.

## 5. Utenti e ruoli

### 5.1 Ruoli

| Ruolo | Ambito | Descrizione |
|---|---|---|
| `SUPERADMIN` | Piattaforma | Crea/disattiva tenant e i loro primi amministratori. Non accede ai dati di dominio dei tenant. |
| `ADMIN` | Tenant | Gestione completa dell'attività, impostazioni, utenti, statistiche. |
| `OPERATORE` | Tenant | Uso quotidiano del gestionale, senza impostazioni né statistiche. |

### 5.2 Modello

- `app_user` (id, email univoca, password_hash bcrypt, nome, attivo, superadmin bool)
- `membership` (user_id, tenant_id, ruolo `ADMIN|OPERATORE`) — un utente può appartenere a più tenant; al login, se ne ha più di uno, sceglie il tenant (fase successiva; inizialmente uno solo).

### 5.3 Matrice permessi

| # | Azione | ADMIN | OPERATORE |
|---|---|:-:|:-:|
| 1 | Visualizzare clienti, prodotti, servizi, fornitori, agenda | ✅ | ✅ |
| 2 | Inserire/modificare clienti | ✅ | ✅ |
| 3 | Eliminare clienti | ✅ | ❌ |
| 4 | Registrare vendite, gestire appuntamenti | ✅ | ✅ |
| 5 | Annullare (soft delete) una vendita | ✅ | ✅ |
| 6 | Gestire catalogo prodotti/servizi, prezzi, fornitori (incluso IBAN) | ✅ | ✅ |
| 7 | Statistiche | ✅ | ❌ |
| 8 | Impostazioni (aspetto, SMTP, agenda, utenti, backup) | ✅ | ❌ |
| 9 | Inviare email ai clienti | ✅ | ✅ |

> L'eliminazione di prodotti, servizi e fornitori ricade nel punto 6: consentita a entrambi i ruoli. Ogni eliminazione è logica (§6.0).

Applicazione: `@PreAuthorize` sui metodi del backend (fonte di verità); route guard e visibilità dei pulsanti in Angular solo per l'esperienza d'uso.

### 5.4 Autenticazione

- `POST /api/auth/login` {email, password, tenantId?} → access token JWT (15 min) nel body + refresh token (14 gg) in cookie `ps_refresh` `HttpOnly; SameSite=Strict; Path=/api/auth`, `Secure` quando la richiesta arriva in HTTPS (anche tramite `X-Forwarded-Proto`).
- Utente con più attività e nessun `tenantId`: la risposta contiene solo `sceltaTenant` e il client ripete il login con l'attività scelta.
- `POST /api/auth/refresh`: **rotazione** del refresh token (il vecchio viene revocato). Riuso di un token già revocato → revoca di tutte le sessioni dell'utente (possibile furto). Il ruolo viene riletto dal DB a ogni rinnovo.
- `POST /api/auth/logout` (revoca + cancellazione cookie), `GET /api/me`, `PUT /api/auth/password`.
- Refresh token salvati solo come hash SHA-256; password con BCrypt.
- Claim JWT: `sub` (user id), `tenant`, `roles`, `nome`, `email`. Firma HS256 con `JWT_SECRET`.
- Limite tentativi: 10 fallimenti in 15 minuti per IP o per email → `429`.
- Password provvisorie (`deve_cambiare_password`): l'interfaccia consente solo il cambio password finché non viene cambiata.
- Primo avvio: se non esistono utenti si crea il SUPERADMIN (`SUPERADMIN_EMAIL` / `SUPERADMIN_PASSWORD`; password generata e scritta nel log se non impostata).
- Frontend: access token solo in memoria; all'avvio la sessione si ripristina con il refresh; l'interceptor rinnova e ripete una volta le richieste che ricevono `401`.
- La vecchia "password di accesso abilitabile" (`TPwd`) è sostituita dal login utente.

---

## 6. Modello dati

Convenzioni: nomi tabelle/colonne in `snake_case`; chiavi `bigint` generate; importi `numeric(12,2)`; aliquote `numeric(5,2)`; date `date`, istanti `timestamptz`; ogni tabella di dominio ha `tenant_id`, `created_at`, `updated_at`, `version` (optimistic locking).

### 6.0 Eliminazione logica (soft delete) — regola generale

- **Nessun record viene mai cancellato fisicamente**: vale per tutte le entità (clienti, prodotti, servizi, fornitori, vendite, appuntamenti, staff, utenti, tenant).
- Ogni tabella ha `deleted_at timestamptz NULL` e `deleted_by bigint NULL` (utente che ha eliminato).
- Implementazione con `@SoftDelete` di Hibernate (o filtro equivalente): le entità eliminate sono escluse automaticamente da tutte le query, e la `DELETE` di JPA diventa un `UPDATE`.
- I vincoli di unicità sono indici parziali `WHERE deleted_at IS NULL`, così un valore eliminato si può reinserire.
- I riferimenti restano validi: una vendita continua a mostrare il cliente o il prodotto eliminato (anche grazie agli snapshot nelle righe).
- Un elemento eliminato non è più selezionabile in nuove vendite o appuntamenti.
- Evoluzione possibile: "cestino" per l'ADMIN con ripristino.

### 6.1 Tenant

| Campo | Tipo | Note |
|---|---|---|
| id | bigint | |
| nome | text | Nome attività (oggi hardcoded come mittente email) |
| attivo | bool | |
| fuso_orario | text | Default `Europe/Rome` |

### 6.2 Cliente (`TClienti`)

| Campo | Tipo | Obbl. | Note |
|---|---|:-:|---|
| nome | text | ✅ | |
| cognome | text | ✅ | |
| giorno_nascita | smallint | | Oggi `COMPLEANNO` = stringa `"DD-MM"` (a volte con spazio iniziale) |
| mese_nascita | smallint | | Anno non gestito: si conserva la scelta |
| telefono | text | | |
| email | text | | Validata solo se presente |
| indirizzo | text | | |
| note | text | | |

### 6.3 Prodotto (`TProdotti`)

| Campo | Tipo | Obbl. | Note |
|---|---|:-:|---|
| data_carico | date | | Oggi `DATA`; usata dalle statistiche come data di acquisto merce |
| marca | text | ✅ | |
| descrizione | text | ✅ | |
| aliquota_iva | numeric(5,2) | ✅ | Oggi stringa `"22%"` |
| quantita | integer | ✅ | ≥ 0 |
| prezzo_netto | numeric(12,2) | ✅ | |
| prezzo_ivato | numeric(12,2) | ✅ | Calcolato, vedi §7.1 |
| prezzo_vendita | numeric(12,2) | ✅ | Impostato liberamente |
| note | text | | |

### 6.4 Servizio (`TServizi`)

Come Prodotto, ma con `nome` al posto di `marca` e **senza** `quantita`.

### 6.5 Fornitore (`TFornitori`)

| Campo | Tipo | Obbl. | Note |
|---|---|:-:|---|
| nome | text | ✅ | |
| indirizzo | text | ✅ | |
| telefono | text | ✅ | |
| email | text | | Facoltativa (oggi NOT NULL); validata solo se presente |
| iban | text | | Validato, vedi §7.4 |
| note | text | | |

### 6.6 Vendita — **nuova struttura testata/righe**

Oggi `TAcquisti` è una tabella piatta: le righe di una stessa sessione sono raggruppate per `(IDCLIENTE, TIMESTAMP)` senza un ID di vendita. Nel nuovo modello:

**`vendita`** (testata)

| Campo | Tipo | Note |
|---|---|---|
| cliente_id | FK cliente | |
| data_ora | timestamptz | Momento della vendita |
| deleted_at, deleted_by | | Annullamento = soft delete (oggi `ISDELETED = 'TRUE'`), vedi §6.0 |
| operatore_id | FK app_user | Chi l'ha registrata |
| totale | numeric(12,2) | Somma delle righe (denormalizzato) |

**`vendita_riga`**

| Campo | Tipo | Note |
|---|---|---|
| vendita_id | FK | |
| tipo | `PRODOTTO` \| `SERVIZIO` | |
| prodotto_id / servizio_id | FK nullable | Esattamente uno valorizzato |
| descrizione | text | **Snapshot**: marca + descrizione / nome al momento della vendita |
| aliquota_iva, prezzo_netto, prezzo_ivato, prezzo_vendita | numeric | **Snapshot** dei prezzi al momento della vendita |
| quantita | integer | Servizi: sempre 1 |
| totale_pagato | numeric(12,2) | Importo effettivo della riga |
| sconto_percentuale | numeric(5,2) | Calcolato, vedi §7.2 |

### 6.7 Staff (nuovo)

Sostituisce il campo testo `OPERATORE` dell'agenda.

| Campo | Tipo | Note |
|---|---|---|
| nome | text | |
| colore | text | Colore di default in agenda (opzionale) |
| user_id | FK app_user nullable | Collegamento opzionale a un login |
| attivo | bool | |

### 6.8 Appuntamento (`TAppuntamenti`, ex DB separato)

| Campo | Tipo | Obbl. | Note |
|---|---|:-:|---|
| titolo | text | ✅ | Auto-generato, vedi §7.6 |
| cliente_id | FK cliente nullable | | Nuovo collegamento |
| nome_cliente | text | ✅ | Mantenuto per clienti occasionali non in anagrafica |
| staff_id | FK staff nullable | | |
| servizio_id | FK servizio nullable | | |
| nome_servizio | text | | Snapshot |
| inizio, fine | timestamptz | ✅ | `fine > inizio` |
| stato | enum | ✅ | `PRENOTATO, CONFERMATO, COMPLETATO, ANNULLATO, ASSENTE` (default `PRENOTATO`) |
| colore | text | | Esadecimale, default `#7B68EE` |
| note | text | | |
| deleted_at, deleted_by | | | Soft delete, vedi §6.0 |

### 6.9 Impostazioni tenant (`TOpzioni`, `TOpzioniAgenda`)

Tabella tipizzata `tenant_settings` (una riga per tenant) invece di chiave/valore:

| Gruppo | Campi | Valori attuali |
|---|---|---|
| Aspetto | logo, colore primario, nome visualizzato | — (nuovo) |
| Agenda | ora_inizio, ora_fine, slot_minuti, durata_default_minuti | 08:00, 21:00, 15, 30 |
| Magazzino | soglia_scorta_bassa | 10 (oggi hardcoded) |
| Email | smtp_host, smtp_porta, smtp_utente, smtp_password, nome_mittente | chiavi `smtpmailgest`, `portmailgest`, `mailgest`, `pwdmailgest` |

La password SMTP è salvata **cifrata** (AES-GCM, chiave da variabile d'ambiente), mai restituita dalle API (solo "impostata sì/no").

---

## 7. Regole di dominio

### 7.1 Prezzi (prodotti e servizi)

- L'utente sceglie se inserire il **netto** o l'**ivato**; l'altro è calcolato:
  - `ivato = ceil((netto × (1 + aliquota/100)) × 100) / 100`
  - `netto = ceil((ivato / (1 + aliquota/100)) × 100) / 100`
- L'arrotondamento è **per eccesso al centesimo** (comportamento attuale). Calcolo in `BigDecimal`, `RoundingMode.CEILING`.
- `prezzo_vendita` è indipendente.
- Il calcolo è replicato nel frontend per l'anteprima, ma il backend ricalcola e fa fede.
- API: la richiesta indica `calcolaDa` (`NETTO` | `IVATO`) e il prezzo corrispondente; l'altro prezzo inviato è ignorato. Se manca il prezzo indicato → errore nel campo `prezzo`.
- Il frontend calcola in centesimi interi (niente errori di arrotondamento dei decimali). Il vecchio programma usava `double` e a volte arrotondava un centesimo in più (es. 10,00 + 22% → 12,21 invece di 12,20).
- In modifica il frontend capisce quale prezzo era stato inserito (se l'ivato corrisponde al netto: netto, altrimenti ivato), così salvare senza toccare i prezzi non li cambia.

### 7.2 Vendite

Flusso (oggi: `ProductPurchaseEditForm` / `ServicePurchaseEditForm`):

1. Dalla scheda cliente si apre una nuova vendita.
2. Si aggiungono prodotti (solo con quantità > 0) e/o servizi al "carrello".
3. Per ogni riga: quantità (servizi = 1) e **sconto %** *oppure* **prezzo personalizzato**:
   - con sconto: `totale = round(prezzo_vendita × qnt × (1 − sconto/100), 2)`
   - con prezzo personalizzato: `totale = prezzo personalizzato`
4. Aggiungere più volte lo stesso articolo somma quantità e totali sulla stessa riga.
5. `sconto_percentuale = (prezzo_vendita × qnt − totale) / (prezzo_vendita × qnt) × 100`, minimo 0.
6. Alla conferma, **in un'unica transazione**: creazione testata + righe e scarico di magazzino (`quantita -= qnt`). Se la giacenza non basta → errore 409, nessuna modifica.
7. **Annullamento** (soft delete): la vendita è esclusa da elenchi e statistiche e, **nella stessa transazione, la giacenza dei prodotti venduti viene ripristinata** (`quantita += qnt`; novità rispetto a oggi). Il ripristino avviene anche se nel frattempo il prodotto è stato eliminato.

### 7.3 Magazzino

- Evidenza in elenco prodotti: `quantita = 0` → rosso; `0 < quantita ≤ soglia` → arancione.
- Nella vendita sono proposti solo i prodotti con `quantita > 0`.

### 7.4 Validazioni

- **Email**: formato valido, solo se presente (eccetto fornitori, vedi §6.5).
- **IBAN**: spazi rimossi, maiuscolo, regex di formato + checksum **Mod-97** (= 1).
- **Obbligatori**: come indicato nelle tabelle del §6. Errori restituiti come `400` con elenco campo → messaggio (formato `ProblemDetail`), evidenziati nel form.

### 7.5 Compleanni

- Avviso nella barra principale se almeno un cliente compie gli anni **nel mese corrente** (calcolato nel fuso orario del tenant, non in UTC).
- Elenco ordinato per giorno; da lì si apre la scheda cliente o si invia un'email di auguri.

### 7.6 Agenda

- Vista settimanale (lun–dom) e giornaliera; orari e slot dalle impostazioni del tenant.
- Creazione con click su uno slot; spostamento e ridimensionamento via drag & drop, agganciati allo slot.
- Titolo auto-generato finché non modificato a mano: `"<servizio> – <cliente>"`, oppure `"Appuntamento <cliente>"`.
- Durata di default dalle impostazioni (oggi 30 min).
- Stati **terminali** (`COMPLETATO`, `ANNULLATO`, `ASSENTE`): l'appuntamento non è trascinabile né modificabile, tranne lo stato. Resa grafica: grigio per annullato/assente, verde pallido per completato.
- Autocomplete del cliente dall'anagrafica (con possibilità di testo libero).
- Ricerca per titolo, cliente, staff, servizio.
- Sovrapposizioni consentite (come oggi), visualizzate affiancate.

### 7.7 Email

- Invio a un singolo cliente dalla sua scheda (oggi pulsante visibile solo se l'SMTP è configurato e il cliente ha un'email).
- Oggetto e messaggio obbligatori; immagine "locandina" opzionale → email HTML con l'immagine inline, altrimenti testo semplice.
- Mittente: nome dell'attività del tenant (oggi hardcoded).
- Invio asincrono con esito restituito al client; log degli invii.

### 7.8 Statistiche (solo ADMIN)

Filtro periodo: sempre / ultimo anno / ultima settimana / intervallo personalizzato.

| Indicatore | Calcolo |
|---|---|
| N. prodotti | In catalogo (senza filtro) o caricati nel periodo (per `data_carico`) |
| Valore inventario | Σ `prezzo_netto × quantita` di tutti i prodotti |
| Entrate | Σ `totale_pagato` delle righe vendute nel periodo, non annullate (**incassato reale**; oggi si usa il netto di listino) |
| Uscite | Σ `prezzo_netto × quantita` dei prodotti caricati nel periodo |
| Utile | Entrate − Uscite |
| Top prodotti | Raggruppati per prodotto: quantità e importo |
| Top clienti | Raggruppati per cliente: n. acquisti e speso |

Grafici: top prodotti e top clienti (barre/torta), bilancio.

Nota: le "Uscite" approssimano gli acquisti di merce con la giacenza attuale dei prodotti caricati nel periodo: una vera gestione degli acquisti da fornitore è un'evoluzione possibile.

Calcolo lato DB (query aggregate), non in memoria come oggi.

### 7.9 Backup

- ADMIN: esportazione dei dati del proprio tenant (JSON/CSV in ZIP).
- Piattaforma: `pg_dump` pianificato su volume (configurazione Docker), fuori dall'applicazione.
- Modalità locale: `pg_dump` giornaliero automatico verso una cartella/disco esterno, con procedura di ripristino (da realizzare prima della prima installazione presso un cliente).

---

## 8. API REST (bozza)

Prefisso `/api`. JSON. Paginazione `?page=&size=&sort=` (max 100 per pagina; ordinamenti consentiti per risorsa, gli altri ignorati), ricerca `?q=`. Gli elenchi paginati rispondono `{elementi, totale, pagina, dimensione}`.

Prodotti e servizi (implementati): ricerca su marca e descrizione in entrambi gli ordini e note (prodotti), nome, descrizione e note (servizi); ordinamento predefinito per marca e descrizione / nome, poi `quantita`, `prezzoVendita`, `dataCarico` (prodotti), `createdAt`, `updatedAt`. `?disponibili=true` restituisce solo i prodotti con quantità > 0. Data di carico assente in inserimento → oggi nel fuso dell'attività. Prezzi come §7.1. Eliminazione consentita anche all'operatore.

Clienti (implementato): ricerca su nome e cognome in entrambi gli ordini, telefono, email; ordinamento `cognome` (predefinito), `nome`, `compleanno`, `createdAt`, `updatedAt`. La modifica (`PUT`) richiede il campo `version` letto in precedenza: se nel frattempo il cliente è cambiato → `409`. Testi ripuliti dagli spazi, email in minuscolo, compleanno con giorno e mese entrambi presenti e data esistente (29 febbraio ammesso), errore nel campo `compleanno`.

| Risorsa | Endpoint | Ruoli |
|---|---|---|
| Auth | `POST /auth/login`, `/auth/refresh`, `/auth/logout`, `PUT /auth/password` | tutti |
| Me | `GET /me` (utente, tenant, ruolo, impostazioni aspetto) | tutti |
| Clienti | `GET/POST /clienti`, `GET/PUT /clienti/{id}` | A, O |
| | `DELETE /clienti/{id}` | A |
| | `GET /clienti/compleanni?mese=` (senza mese: mese corrente nel fuso dell'attività) | A, O |
| | `GET /clienti/{id}/vendite` | A, O |
| | `POST /clienti/{id}/email` (multipart, locandina opzionale) | A, O |
| Prodotti | CRUD `/prodotti`, `?disponibili=true` | A, O |
| Servizi | CRUD `/servizi` | A, O |
| Fornitori | CRUD `/fornitori` | A, O |
| Vendite | `POST /vendite`, `GET /vendite/{id}`, `POST /vendite/{id}/annulla` | A, O |
| Appuntamenti | `GET /appuntamenti?dal=&al=`, CRUD, `PATCH /appuntamenti/{id}/stato` | A, O |
| Staff | `GET /staff` | A, O |
| | CRUD `/staff` | A |
| Statistiche | `GET /statistiche/riepilogo?dal=&al=`, `/top-prodotti`, `/top-clienti` | A |
| Impostazioni | `GET/PUT /impostazioni`, `POST /impostazioni/email/test` | A |
| Utenti tenant | CRUD `/utenti`, `POST /utenti/{id}/reset-password` | A |
| Export | `GET /export` | A |
| Piattaforma | CRUD `/platform/tenants` | SUPERADMIN |

Errori: `ProblemDetail` (RFC 9457). Concorrenza: campo `version`; conflitto → `409`.

---

## 9. Frontend

- Layout responsive (desktop, tablet, smartphone); predisposizione PWA.
- Sezioni: Clienti, Prodotti, Servizi, Fornitori, Agenda, Statistiche*, Impostazioni* (*solo ADMIN).
- Elenchi con ricerca istantanea, ordinamento e paginazione lato server; la riga modificata resta evidenziata dopo il salvataggio.
- Form in dialog (desktop) / pagina intera (mobile); campi obbligatori evidenziati in rosso in caso di errore.
- Badge compleanni nella barra superiore.
- Aspetto (logo, colore primario, nome) personalizzabile per tenant.
- Interfaccia in italiano; formati `it-IT` (`1.234,56 €`, `dd/MM/yyyy`).

---

## 10. Deploy

- `docker-compose.yml` con servizi `postgres` (volume dati), `backend`, `frontend` (nginx; serve Angular e inoltra `/api` al backend).
- Configurazione tramite `.env`: credenziali DB, chiave di firma JWT, chiave di cifratura SMTP, `TZ=Europe/Rome`.
- In locale: porta esposta solo su `127.0.0.1`.
- Online: HTTPS tramite reverse proxy (Caddy/Traefik + Let's Encrypt), backup pianificati.
- Bootstrap: al primo avvio, se non esistono utenti, crea il SUPERADMIN (modalità cloud) oppure l'attività e il suo ADMIN (modalità locale), da variabili d'ambiente (§4.1).
- Installazione presso un cliente (modalità locale): Docker Desktop su Windows (WSL2), `APP_MODE=locale` nel `.env`; in alternativa futura, installazione nativa (PostgreSQL + backend come servizio Windows).

## 11. Migrazione dati

Strumento a riga di comando (modulo del backend) che importa `PSDB.db` e `PSDB_Agenda.db` in un tenant indicato:

- Prezzi: parsing formato italiano (`"18,00 €"` → `18.00`), aliquote (`"22%"` → `22`).
- Compleanni: `" 25-01"` → giorno 25, mese 1.
- Vendite: raggruppamento delle righe `TAcquisti` per `(IDCLIENTE, TIMESTAMP)` → una testata per gruppo.
- Appuntamenti: `OPERATORE` → creazione/collegamento `staff` per nome; `NOMECLIENTE` → collegamento a cliente se c'è una corrispondenza univoca nome+cognome.
- Impostazioni: `TOpzioni`/`TOpzioniAgenda` → `tenant_settings`.
- `TPwd` non migrata (si creano utenti nuovi).
- Report finale di righe importate/scartate.

## 12. Problemi del progetto attuale da non replicare

| Problema | Dove | Soluzione nel nuovo progetto |
|---|---|---|
| Importi e date salvati come stringhe localizzate | tutte le tabelle | Tipi numerici e temporali nativi |
| `ModelAcquisti.updateQuantity` aggiorna **tutte** le righe del cliente per quel prodotto, non solo quelle della vendita corrente | `ModelAcquisti.cs` | Righe identificate da ID |
| `TIMESTAMP` della vendita salvato con `DateTime.ToString()` dipendente dalla lingua di sistema | form vendite | `timestamptz` |
| Scarico magazzino e salvataggio vendita non transazionali | form vendite | Transazione unica nel service |
| Password di accesso e SMTP in chiaro | `TPwd`, `TOpzioni` | bcrypt / cifratura |
| Mese compleanni calcolato in UTC (`STRFTIME('now')`) | `ModelClienti.cs` | Fuso orario del tenant |
| Nome attività hardcoded come mittente email | `SendMailForm.cs` | Impostazione tenant |
| Statistiche calcolate in memoria su tutte le righe | `StatsForm.cs` | Query aggregate |

## 13. Piano di sviluppo

1. ✅ **Scheletro**: Docker Compose con Postgres, Spring Boot con Flyway e health check, Angular con interfaccia a componenti, nginx.
2. ✅ **Fondamenta**: tenant, utenti, membership, login/refresh/logout, `@TenantId`, `@PreAuthorize`, soft delete di base, bootstrap SUPERADMIN, shell Angular con login, guard e gestione attività (SUPERADMIN).
2b. ✅ **Modalità locale**: `APP_MODE=locale` (§4.1), bootstrap dell'attività, ripristino password da riga di comando.
3. ✅ **Clienti**: CRUD completo end-to-end (ricerca, paginazione, ordinamento, compleanni del mese con avviso nella barra), da usare come modello per le altre entità.
4. Prodotti e servizi ✅ (prezzi netto/ivato, giacenza evidenziata con soglia fissa 10 in attesa delle impostazioni), fornitori.
5. Vendite.
6. Staff e agenda.
7. Impostazioni (aspetto, SMTP, agenda) ed email.
8. Statistiche.
9. Migrazione dati e export.
10. PWA e rifinitura per mobile.

## 14. Decisioni prese

| # | Questione | Decisione |
|---|---|---|
| 1 | L'annullamento di una vendita ripristina la giacenza? | Sì (§7.2) |
| 2 | Entrate calcolate sull'incassato reale? | Sì, su `totale_pagato` (§7.8) |
| 3 | Email del fornitore obbligatoria? | No (§6.5) |
| 4 | L'operatore può eliminare prodotti, servizi e fornitori? | Sì (§5.3) |
| 5 | Eliminazione di un cliente con vendite | Soft delete, esteso a **tutte** le entità (§6.0) |
| 6 | Installazione sul PC del cliente senza piattaforma | Modalità `locale` da configurazione, stesso codice (§4.1) |
| 7 | Libreria dei componenti del frontend | Angular Material (MIT) al posto di PrimeNG: dalla versione 22 PrimeNG richiede una chiave di licenza PrimeUI (gratuita solo sotto certi limiti, rinnovo annuale) |
