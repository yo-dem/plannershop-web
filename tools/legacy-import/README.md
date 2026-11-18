# Importazione dal vecchio PlannerShop

`import_legacy.py` copia clienti, fornitori, prodotti e servizi dal database del vecchio
programma desktop (`PSDB.db`, SQLite) in un'attività di PlannerShop Web.

Lo script **legge soltanto** il vecchio file e scrive su standard output le istruzioni SQL, da
eseguire con `psql`. Gli avvisi e il riepilogo vanno su standard error. Serve solo Python 3.9+,
senza librerie da installare.

## Uso

```bash
# 1. id dell'attività che riceve i dati
cd docker
docker compose exec -T postgres psql -U plannershop -d plannershop -c "select id, name from tenant"

# 2. genera l'SQL (qui per l'attività con id 1) e leggi gli avvisi
cd ..
python3 tools/legacy-import/import_legacy.py /percorso/PSDB.db --tenant-id 1 > import.sql

# 3. eseguilo
docker compose -f docker/docker-compose.yml exec -T postgres \
    psql -U plannershop -d plannershop -v ON_ERROR_STOP=1 < import.sql
```

Tutto avviene in un'unica transazione: se qualcosa va storto non viene importato niente.

**Solo in un'attività vuota.** Se l'attività ha già clienti, fornitori, prodotti o servizi (per
esempio i dati demo), l'SQL si ferma con un errore e non importa nulla, così i dati vecchi non si
mescolano per sbaglio con quelli attuali. Per aggiungerli comunque usa `--merge`: le righe già
presenti (stesso nome, maiuscole a parte) vengono saltate, quindi si può rieseguire senza creare
doppioni.

Non salvare nel repository né il vecchio `PSDB.db` né l'SQL generato: contengono dati personali
dei clienti (la cartella ha un `.gitignore` apposta).

## Cosa viene importato

| Vecchia tabella | Nuova tabella | Note |
|---|---|---|
| `TClienti` | `customer` | compleanno `GG-MM` → giorno e mese; email in minuscolo |
| `TFornitori` | `supplier` | IBAN senza spazi, verificato; indirizzo o telefono mancanti → `-` |
| `TProdotti` | `product` | prezzi `18,00 €` → `18.00`; IVA `22%` → `22`; prezzo ivato ricalcolato se manca |
| `TServizi` | `service_item` | come i prodotti; descrizione mancante → `-` |

I valori che non si possono salvare nel loro campo (email o IBAN non validi, compleanno
inesistente) finiscono nelle **note**, con un avviso. Le righe senza nome vengono saltate.

Non importati per ora: acquisti (`TAcquisti`, quando ci saranno le vendite, passo 9 del piano),
opzioni (`TOpzioni`, contengono la password SMTP), password (`TPwd`), agenda (`PSDB_Agenda.db`).

## Test

```bash
cd tools/legacy-import && python3 -m unittest
```

I test usano un piccolo database finto con lo stesso schema del vecchio programma.
