-- Initial migration.
-- Flyway applies the V<n>__<description>.sql files in order, once each,
-- and records them in the flyway_schema_history table.
-- RULE: a migration that has been applied is never modified; add a new file instead.
--
-- Domain tables (tenants, users, customers, ...) start from V2.

CREATE TABLE app_info (
    info_key    text PRIMARY KEY,
    info_value  text NOT NULL
);

INSERT INTO app_info (info_key, info_value) VALUES ('schema_created_at', now()::text);
