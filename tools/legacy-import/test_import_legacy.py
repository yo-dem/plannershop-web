"""Tests of import_legacy.py on a small fake PSDB.db (no real data).

    cd tools/legacy-import && python3 -m unittest
"""
import os
import sqlite3
import tempfile
import unittest
from decimal import Decimal

import import_legacy as il

# Same tables as the old PlannerShop (only the columns the script reads).
SCHEMA = """
CREATE TABLE TClienti (IDCLIENTE INTEGER PRIMARY KEY, NOME TEXT, COGNOME TEXT, COMPLEANNO TEXT,
    TELEFONO TEXT, EMAIL TEXT, INDIRIZZO TEXT, NOTE TEXT);
CREATE TABLE TFornitori (IDFORNITORE INTEGER PRIMARY KEY, NOME TEXT, INDIRIZZO TEXT, TELEFONO TEXT,
    EMAIL TEXT, NOTE TEXT, IBAN TEXT);
CREATE TABLE TProdotti (IDPRODOTTO INTEGER PRIMARY KEY, DATA TEXT, MARCA TEXT, DESCRIZIONE TEXT,
    ALIQUOTA TEXT, QNT TEXT, PREZZO_NETTO TEXT, PREZZO_IVATO TEXT, NOTE TEXT, PREZZO_VENDITA TEXT);
CREATE TABLE TServizi (IDSERVIZIO INTEGER PRIMARY KEY, DATA TEXT, NOME TEXT, DESCRIZIONE TEXT,
    ALIQUOTA TEXT, PREZZO_NETTO TEXT, PREZZO_IVATO TEXT, PREZZO_VENDITA TEXT, NOTE TEXT);
CREATE TABLE TAcquisti (IDACQUISTO INTEGER PRIMARY KEY, NOME TEXT);
"""


class ConversionTests(unittest.TestCase):

    def test_birthday(self):
        self.assertEqual(il.birthday(" 15-03"), (15, 3))
        self.assertEqual(il.birthday("29-02"), (29, 2))
        self.assertIsNone(il.birthday("31-04"))
        self.assertIsNone(il.birthday("15-13"))
        self.assertIsNone(il.birthday(""))
        self.assertIsNone(il.birthday(None))

    def test_amount(self):
        self.assertEqual(il.amount("18,00 €"), Decimal("18.00"))
        self.assertEqual(il.amount("1.234,5 €"), Decimal("1234.50"))
        self.assertEqual(il.amount("9.9"), Decimal("9.90"))
        self.assertIsNone(il.amount(""))
        self.assertIsNone(il.amount("abc"))

    def test_vat_rate(self):
        self.assertEqual(il.vat_rate("22%"), Decimal("22.00"))
        self.assertEqual(il.vat_rate("4,5%"), Decimal("4.50"))
        self.assertIsNone(il.vat_rate("120%"))
        self.assertIsNone(il.vat_rate(None))

    def test_iban(self):
        self.assertEqual(il.iban(" it60 x054 2811 1010 0000 0123 456 "), "IT60X0542811101000000123456")
        self.assertTrue(il.iban_is_valid("IT60X0542811101000000123456"))
        self.assertFalse(il.iban_is_valid("IT61X0542811101000000123456"))

    def test_sql_literal(self):
        self.assertEqual(il.sql_literal("Dell'Acqua"), "'Dell''Acqua'")
        self.assertEqual(il.sql_literal(None), "NULL")
        self.assertEqual(il.sql_literal(Decimal("1.50")), "1.50")
        self.assertEqual(il.sql_literal("a\nb\\c"), "E'a\\nb\\\\c'")


class GenerateTests(unittest.TestCase):

    def setUp(self):
        fd, self.path = tempfile.mkstemp(suffix=".db")
        os.close(fd)
        con = sqlite3.connect(self.path)
        con.executescript(SCHEMA)
        con.executemany("INSERT INTO TClienti VALUES (?,?,?,?,?,?,?,?)", [
            (1, " Anna ", "Dell'Acqua", " 15-03", "333 000 0001", "Anna@Example.com", "", ""),
            (2, "Bruno", "Verdi", "31-02", "", "not-an-email", None, "VIP"),
            (3, "", "Senza nome", None, None, None, None, None),
        ])
        con.executemany("INSERT INTO TFornitori VALUES (?,?,?,?,?,?,?)", [
            (1, "Ingrosso srl", "Via Roma 1", "051 000 0000", "", None, "IT60 X054 2811 1010 0000 0123 456"),
            (2, "Senza IBAN valido", "", "", "info@example.com", "", "IT00BAD"),
        ])
        con.executemany("INSERT INTO TProdotti VALUES (?,?,?,?,?,?,?,?,?,?)", [
            (1, "2025-01-10", "DERMALUXE", "CREMA 50ML", "22%", "30", "18,00 €", "", "", "25,00 €"),
            (2, "10/01/2025", "SKINLAB", "MASCHERA", "", "-3", "6,50 €", "7,93 €", "", ""),
        ])
        con.executemany("INSERT INTO TServizi VALUES (?,?,?,?,?,?,?,?,?)", [
            (1, "2025-01-10", "PULIZIA VISO", "", "22%", "30,00 €", "36,60 €", "45,00 €", "60 min"),
        ])
        con.execute("INSERT INTO TAcquisti VALUES (1, 'x')")
        con.commit()
        con.close()
        self.report = il.Report()
        self.sql = il.generate(self.path, 7, self.report)

    def tearDown(self):
        os.remove(self.path)

    def line(self, table, contains):
        found = [l for l in self.sql.splitlines() if l.startswith(f"INSERT INTO {table} ") and contains in l]
        self.assertEqual(len(found), 1, f"{table} with {contains!r}")
        return found[0]

    def test_single_transaction_checking_the_business(self):
        lines = self.sql.splitlines()
        self.assertEqual(lines[0], "BEGIN;")
        self.assertIn("RAISE EXCEPTION 'Business with id 7 not found'", lines[1])
        # by default only into an empty business
        self.assertIn("EXISTS (SELECT 1 FROM customer WHERE tenant_id = 7 AND deleted_at IS NULL)", lines[2])
        self.assertIn("RAISE EXCEPTION 'Business with id 7 already has data", lines[2])
        self.assertEqual(lines[-1], "COMMIT;")

    def test_merge_skips_the_empty_check(self):
        sql = il.generate(self.path, 7, il.Report(), merge=True)
        self.assertNotIn("already has data", sql)
        self.assertEqual(sql.count("INSERT INTO"), self.sql.count("INSERT INTO"))

    def test_customers(self):
        anna = self.line("customer", "'Anna'")
        self.assertIn("SELECT 7, 'Anna', 'Dell''Acqua', 15, 3, '333 000 0001', 'anna@example.com', NULL, NULL", anna)
        # skipped when already present: same names, ignoring case, not deleted
        self.assertIn("lower(first_name) = lower('Anna') AND lower(last_name) = lower('Dell''Acqua')", anna)
        self.assertIn("tenant_id = 7 AND deleted_at IS NULL", anna)
        # invalid birthday and e-mail kept in the notes
        bruno = self.line("customer", "'Bruno'")
        self.assertIn("NULL, NULL, NULL, NULL, NULL, E'VIP\\n31-02\\nnot-an-email'", bruno)
        self.assertNotIn("Senza nome", self.sql)
        self.assertEqual(self.report.read["customers"], 3)

    def test_suppliers(self):
        self.assertIn("'IT60X0542811101000000123456'", self.line("supplier", "'Ingrosso srl'"))
        other = self.line("supplier", "'Senza IBAN valido'")
        self.assertIn("'-', '-', 'info@example.com', NULL, 'IBAN IT00BAD'", other)

    def test_products_and_services(self):
        crema = self.line("product", "'CREMA 50ML'")
        # gross price recalculated from the net price
        self.assertIn("'2025-01-10', 'DERMALUXE', 'CREMA 50ML', 30, 22.00, 18.00, 21.96, 25.00, NULL", crema)
        maschera = self.line("product", "'MASCHERA'")
        # wrong date and quantity, missing VAT and sale price: defaults
        self.assertIn("NULL, 'SKINLAB', 'MASCHERA', 0, 22, 6.50, 7.93, 7.93, NULL", maschera)
        self.assertIn("'PULIZIA VISO', '-', 22.00, 30.00, 36.60, 45.00, '60 min'", self.line("service_item", "PULIZIA"))

    def test_warnings(self):
        text = "\n".join(self.report.warnings)
        for expected in ["TClienti #2: birthday", "TClienti #2: e-mail", "TClienti #3: first or last name",
                         "TFornitori #2: IBAN", "TProdotti #2: quantity", "TProdotti #2: date",
                         "TProdotti #2: VAT rate", "TProdotti #2: sale price", "TAcquisti: 1 purchases"]:
            self.assertIn(expected, text)


if __name__ == "__main__":
    unittest.main()
