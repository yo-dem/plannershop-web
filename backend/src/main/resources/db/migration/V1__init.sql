-- Migrazione iniziale.
-- Flyway applica i file V<n>__<descrizione>.sql in ordine, una sola volta,
-- e registra quelli eseguiti nella tabella flyway_schema_history.
-- REGOLA: una migrazione già applicata non si modifica mai; si aggiunge un nuovo file.
--
-- Le tabelle di dominio (tenant, utenti, clienti, ...) arrivano dalla V2 in poi.

CREATE TABLE app_info (
    chiave  text PRIMARY KEY,
    valore  text NOT NULL
);

INSERT INTO app_info (chiave, valore) VALUES ('schema_creato_il', now()::text);
