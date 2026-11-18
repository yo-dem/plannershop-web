#!/usr/bin/env python3
"""Import the data of the old desktop PlannerShop (SQLite file PSDB.db) into a
business of PlannerShop Web.

The script only READS the old file and prints SQL on standard output, to be
run with psql on the PostgreSQL database (see README.md in this folder).
Warnings and a summary go to standard error.

Imported: customers (TClienti), suppliers (TFornitori), products (TProdotti),
services (TServizi). Not imported (yet): purchases (TAcquisti, step 9 of the
plan, after sales exist), options (TOpzioni: e-mail settings with the SMTP
password), password (TPwd), agenda (PSDB_Agenda.db).

The SQL runs in a single transaction and stops if the business does not exist
or already has customers, suppliers, products or services, so old data are
never mixed by mistake with the current ones (e.g. demo data). With --merge it
adds the old data to a business that already has some: rows already present
(same name, ignoring case, not deleted) are skipped, so it can be run again.

Only the standard library is used (Python 3.9+).
"""
import argparse
import calendar
import re
import sqlite3
import sys
from dataclasses import dataclass, field
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP

DEFAULT_VAT_RATE = Decimal("22")
CENT = Decimal("0.01")
EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


@dataclass
class Report:
    """Counts and warnings, printed on standard error at the end."""
    read: dict = field(default_factory=dict)
    warnings: list = field(default_factory=list)

    def warn(self, table, row_id, message):
        self.warnings.append(f"{table} #{row_id}: {message}")


# --- conversions of the old text values ---------------------------------------

def text(value):
    """Trimmed text; empty text becomes None."""
    value = (value or "").strip()
    return value or None


def birthday(value):
    """' 15-03' (day-month) → (15, 3); None if missing or not a real date (29 Feb allowed)."""
    m = re.fullmatch(r"(\d{1,2})[-/.](\d{1,2})", (value or "").strip())
    if not m:
        return None
    day, month = int(m[1]), int(m[2])
    if not 1 <= month <= 12 or not 1 <= day <= calendar.monthrange(2024, month)[1]:
        return None
    return day, month


def amount(value):
    """'1.234,50 €' → Decimal('1234.50'); None if missing or not a number."""
    s = (value or "").replace("€", "").replace(" ", "").replace(" ", "")
    if not s:
        return None
    s = s.replace(".", "").replace(",", ".") if "," in s else s
    try:
        return Decimal(s).quantize(CENT, ROUND_HALF_UP)
    except InvalidOperation:
        return None


def vat_rate(value):
    """'22%' → Decimal('22.00'); None if not a number between 0 and 100."""
    rate = amount((value or "").replace("%", ""))
    return rate if rate is not None and 0 <= rate <= 100 else None


def quantity(value):
    """'30' → 30; None if not a whole number."""
    s = (value or "").strip()
    return int(s) if re.fullmatch(r"-?\d+", s) else None


def iban(value):
    """IBAN without spaces, upper case; None if missing. Valid = format + Mod-97 check."""
    s = re.sub(r"\s", "", value or "").upper()
    return s or None


def iban_is_valid(s):
    if not re.fullmatch(r"[A-Z]{2}\d{2}[A-Z0-9]{11,30}", s):
        return False
    digits = "".join(str(int(c, 36)) for c in s[4:] + s[:4])
    return int(digits) % 97 == 1


def add_to_notes(notes, extra):
    """Old values that cannot be stored in their field are kept in the notes."""
    return f"{notes}\n{extra}" if notes else extra


# --- SQL ------------------------------------------------------------------------

def sql_literal(v):
    if v is None:
        return "NULL"
    if isinstance(v, (int, Decimal)):
        return str(v)
    s = str(v).replace("'", "''")
    if "\n" in s or "\r" in s or "\\" in s:
        # E'...' string: line breaks written as \n, so every statement stays on one line
        s = s.replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n")
        return "E'" + s + "'"
    return "'" + s + "'"


class SqlWriter:
    def __init__(self, tenant_id):
        self.tenant_id = tenant_id
        self.lines = []

    def insert(self, table, values, natural_key):
        """INSERT ... WHERE NOT EXISTS: skips the row if already present in the business."""
        columns = ", ".join(["tenant_id", *values])
        literals = ", ".join([str(self.tenant_id), *(sql_literal(v) for v in values.values())])
        same = " AND ".join(
            f"lower({c}) = lower({sql_literal(values[c])})" if isinstance(values[c], str)
            else f"{c} IS NOT DISTINCT FROM {sql_literal(values[c])}"
            for c in natural_key)
        self.lines.append(
            f"INSERT INTO {table} ({columns}) SELECT {literals} WHERE NOT EXISTS "
            f"(SELECT 1 FROM {table} WHERE tenant_id = {self.tenant_id} AND deleted_at IS NULL AND {same});")


# --- tables ---------------------------------------------------------------------

def rows(con, table, order):
    return con.execute(f"SELECT * FROM {table} ORDER BY {order}").fetchall()


def prices(r, table, row_id, report):
    """VAT rate and prices; the gross price is recalculated when missing."""
    rate = vat_rate(r["ALIQUOTA"])
    if rate is None:
        report.warn(table, row_id, f"VAT rate {r['ALIQUOTA']!r} not valid: {DEFAULT_VAT_RATE}% used")
        rate = DEFAULT_VAT_RATE
    net = amount(r["PREZZO_NETTO"])
    gross = amount(r["PREZZO_IVATO"])
    if net is None and gross is None:
        report.warn(table, row_id, "net and gross price missing: 0 used")
        net = gross = Decimal("0.00")
    elif net is None:
        net = (gross * 100 / (100 + rate)).quantize(CENT, ROUND_HALF_UP)
    elif gross is None:
        gross = (net * (100 + rate) / 100).quantize(CENT, ROUND_HALF_UP)
    sale = amount(r["PREZZO_VENDITA"])
    if sale is None:
        report.warn(table, row_id, "sale price missing: gross price used")
        sale = gross
    return {"vat_rate": rate, "net_price": net, "gross_price": gross, "sale_price": sale}


def import_customers(con, out, report):
    data = rows(con, "TClienti", "IDCLIENTE")
    report.read["customers"] = len(data)
    for r in data:
        rid, notes = r["IDCLIENTE"], text(r["NOTE"])
        first, last = text(r["NOME"]), text(r["COGNOME"])
        if not first or not last:
            report.warn("TClienti", rid, "first or last name missing: skipped")
            continue
        bday = birthday(r["COMPLEANNO"])
        if bday is None and text(r["COMPLEANNO"]):
            report.warn("TClienti", rid, f"birthday {r['COMPLEANNO']!r} not valid: moved to the notes")
            notes = add_to_notes(notes, text(r["COMPLEANNO"]))
        email = text(r["EMAIL"])
        if email and not EMAIL_RE.match(email):
            report.warn("TClienti", rid, "e-mail not valid: moved to the notes")
            notes, email = add_to_notes(notes, email), None
        out.insert("customer", {
            "first_name": first, "last_name": last,
            "birth_day": bday[0] if bday else None, "birth_month": bday[1] if bday else None,
            "phone": text(r["TELEFONO"]), "email": email.lower() if email else None,
            "address": text(r["INDIRIZZO"]), "notes": notes,
        }, ["first_name", "last_name"])


def import_suppliers(con, out, report):
    data = rows(con, "TFornitori", "IDFORNITORE")
    report.read["suppliers"] = len(data)
    for r in data:
        rid, notes = r["IDFORNITORE"], text(r["NOTE"])
        name = text(r["NOME"])
        if not name:
            report.warn("TFornitori", rid, "name missing: skipped")
            continue
        code = iban(r["IBAN"])
        if code and not iban_is_valid(code):
            report.warn("TFornitori", rid, "IBAN not valid: moved to the notes")
            notes, code = add_to_notes(notes, "IBAN " + code), None
        email = text(r["EMAIL"])
        if email and not EMAIL_RE.match(email):
            report.warn("TFornitori", rid, "e-mail not valid: moved to the notes")
            notes, email = add_to_notes(notes, email), None
        # Address and phone are required in the new application.
        out.insert("supplier", {
            "name": name, "address": text(r["INDIRIZZO"]) or "-", "phone": text(r["TELEFONO"]) or "-",
            "email": email.lower() if email else None, "iban": code, "notes": notes,
        }, ["name"])


def import_products(con, out, report):
    data = rows(con, "TProdotti", "IDPRODOTTO")
    report.read["products"] = len(data)
    for r in data:
        rid = r["IDPRODOTTO"]
        brand, description = text(r["MARCA"]), text(r["DESCRIZIONE"])
        if not brand or not description:
            report.warn("TProdotti", rid, "brand or description missing: skipped")
            continue
        qty = quantity(r["QNT"])
        if qty is None or qty < 0:
            report.warn("TProdotti", rid, f"quantity {r['QNT']!r} not valid: 0 used")
            qty = 0
        stock_date = text(r["DATA"])
        if stock_date and not re.fullmatch(r"\d{4}-\d{2}-\d{2}", stock_date):
            report.warn("TProdotti", rid, f"date {stock_date!r} not valid: left empty")
            stock_date = None
        out.insert("product", {
            "stock_date": stock_date, "brand": brand, "description": description, "quantity": qty,
            **prices(r, "TProdotti", rid, report), "notes": text(r["NOTE"]),
        }, ["brand", "description"])


def import_services(con, out, report):
    data = rows(con, "TServizi", "IDSERVIZIO")
    report.read["services"] = len(data)
    for r in data:
        rid = r["IDSERVIZIO"]
        name = text(r["NOME"])
        if not name:
            report.warn("TServizi", rid, "name missing: skipped")
            continue
        out.insert("service_item", {
            "name": name, "description": text(r["DESCRIZIONE"]) or "-",
            **prices(r, "TServizi", rid, report), "notes": text(r["NOTE"]),
        }, ["name"])


BUSINESS_TABLES = ["customer", "supplier", "product", "service_item"]


def generate(legacy_db, tenant_id, report, merge=False):
    """SQL that imports the old data into the business tenant_id (empty unless merge)."""
    con = sqlite3.connect(f"file:{legacy_db}?mode=ro", uri=True)
    con.row_factory = sqlite3.Row
    out = SqlWriter(tenant_id)
    out.lines += [
        "BEGIN;",
        # Stops everything if the business does not exist.
        "DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM tenant WHERE id = %d) THEN "
        "RAISE EXCEPTION 'Business with id %d not found'; END IF; END $$;" % (tenant_id, tenant_id),
    ]
    if not merge:
        has_data = " OR ".join(
            f"EXISTS (SELECT 1 FROM {t} WHERE tenant_id = {tenant_id} AND deleted_at IS NULL)" for t in BUSINESS_TABLES)
        out.lines.append(
            f"DO $$ BEGIN IF {has_data} THEN RAISE EXCEPTION 'Business with id {tenant_id} already has data: "
            f"nothing imported (use --merge to add the old data anyway)'; END IF; END $$;")
    import_customers(con, out, report)
    import_suppliers(con, out, report)
    import_products(con, out, report)
    import_services(con, out, report)
    purchases = con.execute("SELECT count(*) FROM TAcquisti").fetchone()[0]
    if purchases:
        report.warnings.append(f"TAcquisti: {purchases} purchases NOT imported (sales are not available yet)")
    out.lines.append("COMMIT;")
    con.close()
    return "\n".join(out.lines) + "\n"


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("legacy_db", help="path of the old PSDB.db")
    parser.add_argument("--tenant-id", type=int, required=True,
                        help="id of the business that receives the data (table tenant)")
    parser.add_argument("--merge", action="store_true",
                        help="also import into a business that already has data, skipping rows already present")
    args = parser.parse_args(argv)
    report = Report()
    sys.stdout.write(generate(args.legacy_db, args.tenant_id, report, args.merge))
    print("Read from the old database: " + ", ".join(f"{n} {k}" for k, n in report.read.items()), file=sys.stderr)
    for w in report.warnings:
        print("WARNING " + w, file=sys.stderr)
    if args.merge:
        print("--merge: rows already present in the business are skipped when the SQL runs.", file=sys.stderr)
    else:
        print("The SQL stops if the business already has data (see --merge).", file=sys.stderr)


if __name__ == "__main__":
    main()
