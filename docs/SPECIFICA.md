# PlannerShop Web — Specifica funzionale e tecnica

> Versione 0.1 — 10/11/2026
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
| Backend | Java 21, Spring Boot 3, Maven |
| Persistenza | Spring Data JPA (Hibernate 6), PostgreSQL 16, Flyway |
| Sicurezza | Spring Security, `oauth2-resource-server` con JWT firmati dal backend, refresh token in cookie `httpOnly` |
| API docs | springdoc-openapi → generazione automatica del client TypeScript |
| Email | Spring Boot Mail (SMTP per tenant) |
| Frontend | Angular (ultima versione stabile), TypeScript, PrimeNG |
| Agenda | FullCalendar (`@fullcalendar/angular`) |
| Grafici | Chart.js (via PrimeNG Chart) |
| Deploy | Docker Compose: `postgres`, `backend`, `frontend` (nginx + reverse proxy `/api`) |

## 3. Struttura del repository

```
plannershop-web/
├── backend/     Spring Boot
├── frontend/    Angular
├── docker/      docker-compose.yml, nginx.conf, .env.example
└── docs/        specifica e decisioni architetturali
```

---

## 4. Multi-tenancy

- **Strategia**: schema condiviso, colonna `tenant_id` su ogni tabella di dominio.
- Le entità di dominio hanno un campo annotato `@TenantId` (Hibernate 6): il filtro `tenant_id = ?` è applicato automaticamente a ogni query e il valore è impostato automaticamente in inserimento.
- Il tenant corrente è letto **esclusivamente** dal token dell'utente autenticato (claim `tenant`), tramite un `CurrentTenantIdentifierResolver`. Il client non invia mai il tenant come parametro.
- Unicità e indici sono sempre composti con `tenant_id` (es. `UNIQUE (tenant_id, email)`).
- Evoluzione possibile: Row Level Security di PostgreSQL come seconda barriera.

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

> Nota: l'eliminazione di prodotti, servizi e fornitori ricade nel punto 6 (consentita a entrambi). Da riconfermare.

Applicazione: `@PreAuthorize` sui metodi del backend (fonte di verità); route guard e visibilità dei pulsanti in Angular solo per l'esperienza d'uso.

### 5.4 Autenticazione

- `POST /api/auth/login` → access token JWT (durata ~15 min) nel body + refresh token (durata ~7–30 gg) in cookie `httpOnly; Secure; SameSite=Strict`.
- `POST /api/auth/refresh`, `POST /api/auth/logout` (revoca del refresh token, salvato hashato in DB).
- Claim JWT: `sub` (user id), `tenant`, `role`.
- Limitazione dei tentativi di login (rate limit per IP/email).
- Cambio password da parte dell'utente; reset password da parte dell'ADMIN per i propri operatori.
- La vecchia "password di accesso abilitabile" (`TPwd`) è sostituita dal login utente: non esiste più la modalità senza password.

---

## 6. Modello dati

Convenzioni: nomi tabelle/colonne in `snake_case`; chiavi `bigint` generate; importi `numeric(12,2)`; aliquote `numeric(5,2)`; date `date`, istanti `timestamptz`; ogni tabella di dominio ha `tenant_id`, `created_at`, `updated_at`, `version` (optimistic locking).

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
| email | text | ✅ | Nel DB oggi NOT NULL; da confermare se resta obbligatoria |
| iban | text | | Validato, vedi §7.4 |
| note | text | | |

### 6.6 Vendita — **nuova struttura testata/righe**

Oggi `TAcquisti` è una tabella piatta: le righe di una stessa sessione sono raggruppate per `(IDCLIENTE, TIMESTAMP)` senza un ID di vendita. Nel nuovo modello:

**`vendita`** (testata)

| Campo | Tipo | Note |
|---|---|---|
| cliente_id | FK cliente | |
| data_ora | timestamptz | Momento della vendita |
| annullata | bool | Soft delete (oggi `ISDELETED = 'TRUE'`) |
| annullata_il, annullata_da | | Tracciamento |
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
| eliminato | bool | | Soft delete |

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
7. **Annullamento** (soft delete): la vendita resta visibile come annullata ed è esclusa dalle statistiche. **Da decidere**: se l'annullamento debba ripristinare la giacenza dei prodotti (oggi non avviene).

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

| Indicatore | Calcolo attuale |
|---|---|
| N. prodotti | In catalogo (senza filtro) o caricati nel periodo (per `data_carico`) |
| Valore inventario | Σ `prezzo_netto × quantita` di tutti i prodotti |
| Entrate | Σ `prezzo_netto × qnt` delle righe vendute nel periodo, non annullate |
| Uscite | Σ `prezzo_netto × quantita` dei prodotti caricati nel periodo |
| Utile | Entrate − Uscite |
| Top prodotti | Raggruppati per prodotto: quantità e importo |
| Top clienti | Raggruppati per cliente: n. acquisti e speso |

Grafici: top prodotti e top clienti (barre/torta), bilancio.

**Da decidere**: le "Entrate" oggi usano il prezzo netto di listino; più corretto usare `totale_pagato` (incassato reale). Le "Uscite" approssimano gli acquisti di merce con la giacenza attuale dei prodotti caricati nel periodo: una vera gestione degli acquisti da fornitore è un'evoluzione possibile.

Calcolo lato DB (query aggregate), non in memoria come oggi.

### 7.9 Backup

- ADMIN: esportazione dei dati del proprio tenant (JSON/CSV in ZIP).
- Piattaforma: `pg_dump` pianificato su volume (configurazione Docker), fuori dall'applicazione.

---

## 8. API REST (bozza)

Prefisso `/api`. JSON. Paginazione `?page=&size=&sort=`, ricerca `?q=`.

| Risorsa | Endpoint | Ruoli |
|---|---|---|
| Auth | `POST /auth/login`, `/auth/refresh`, `/auth/logout`, `PUT /auth/password` | tutti |
| Me | `GET /me` (utente, tenant, ruolo, impostazioni aspetto) | tutti |
| Clienti | `GET/POST /clienti`, `GET/PUT /clienti/{id}` | A, O |
| | `DELETE /clienti/{id}` | A |
| | `GET /clienti/compleanni?mese=` | A, O |
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
- Bootstrap: al primo avvio, se non esistono utenti, crea il SUPERADMIN da variabili d'ambiente.

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

1. **Scheletro**: Docker Compose con Postgres, Spring Boot con Flyway e health check, Angular con PrimeNG, nginx.
2. **Fondamenta**: tenant, utenti, membership, login/refresh/logout, `@TenantId`, `@PreAuthorize`, bootstrap SUPERADMIN, shell Angular con login e guard.
3. **Clienti**: CRUD completo end-to-end, da usare come modello per le altre entità.
4. Prodotti, servizi, fornitori.
5. Vendite.
6. Staff e agenda.
7. Impostazioni (aspetto, SMTP, agenda) ed email.
8. Statistiche.
9. Migrazione dati e export.
10. PWA e rifinitura per mobile.

## 14. Questioni aperte

1. L'annullamento di una vendita deve ripristinare la giacenza?
2. Statistiche: le entrate vanno calcolate sull'incassato reale (`totale_pagato`) invece che sul netto di listino?
3. L'email del fornitore resta obbligatoria?
4. L'eliminazione di prodotti, servizi e fornitori è consentita anche all'operatore (punto 6 della matrice)?
5. Eliminazione di un cliente con vendite associate: bloccarla, oppure soft delete del cliente?
