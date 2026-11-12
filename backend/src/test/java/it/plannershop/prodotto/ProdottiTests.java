package it.plannershop.prodotto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.Test;

import it.plannershop.AbstractAttivitaTest;

class ProdottiTests extends AbstractAttivitaTest {

    private static final String URL = "/api/prodotti";

    /** Prodotto con prezzo netto 10, IVA 22%, vendita 15. */
    private static String prodotto(String marca, String descrizione, int quantita) {
        return """
                {"marca":"%s","descrizione":"%s","quantita":%d,"aliquotaIva":22,
                 "calcolaDa":"NETTO","prezzoNetto":10,"prezzoVendita":15}""".formatted(marca, descrizione, quantita);
    }

    @Test
    void inserimentoConCalcoloDellIvatoELetturaEModifica() throws Exception {
        String creato = invia(post(URL), operatore, """
                {"marca":" Davines ","descrizione":"Shampoo 250 ml","quantita":12,"aliquotaIva":22,
                 "calcolaDa":"NETTO","prezzoNetto":10,"prezzoIvato":999,"prezzoVendita":18.50,"note":""}""")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.marca").value("Davines"))
                .andExpect(jsonPath("$.prezzoNetto").value(10.00))
                .andExpect(jsonPath("$.prezzoIvato").value(12.20)) // ricalcolato: il 999 inviato è ignorato
                .andExpect(jsonPath("$.prezzoVendita").value(18.50))
                .andExpect(jsonPath("$.note").doesNotExist())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();
        long id = id(creato);

        // Senza data di carico: oggi, nel fuso orario dell'attività.
        chiama(get(URL + "/" + id), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataCarico").value(LocalDate.now(ZoneId.of("Europe/Rome")).toString()))
                .andExpect(jsonPath("$.quantita").value(12));

        // Modifica partendo dall'ivato: il netto viene calcolato.
        invia(put(URL + "/" + id), operatore, """
                {"dataCarico":"2026-03-15","marca":"Davines","descrizione":"Shampoo 250 ml","quantita":0,
                 "aliquotaIva":22,"calcolaDa":"IVATO","prezzoIvato":18,"prezzoVendita":20,"version":0}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataCarico").value("2026-03-15"))
                .andExpect(jsonPath("$.prezzoNetto").value(14.76))
                .andExpect(jsonPath("$.prezzoIvato").value(18.00))
                .andExpect(jsonPath("$.quantita").value(0))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void datiNonValidiRestituisconoGliErroriPerCampo() throws Exception {
        invia(post(URL), admin, """
                {"marca":" ","quantita":-1,"aliquotaIva":101,"calcolaDa":"NETTO","prezzoIvato":10,
                 "prezzoVendita":12.345}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.marca").value("Inserisci la marca"))
                .andExpect(jsonPath("$.errori.descrizione").value("Inserisci la descrizione"))
                .andExpect(jsonPath("$.errori.quantita").value("Non può essere negativa"))
                .andExpect(jsonPath("$.errori.aliquotaIva").value("Aliquota non valida"))
                .andExpect(jsonPath("$.errori.prezzo").value("Inserisci il prezzo")) // netto mancante
                .andExpect(jsonPath("$.errori.prezzoVendita").value("Importo non valido"));

        invia(post(URL), admin, """
                {"marca":"A","descrizione":"B","quantita":1,"aliquotaIva":22,"prezzoNetto":10,"prezzoVendita":-1}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.calcolaDa").exists())
                .andExpect(jsonPath("$.errori.prezzoVendita").value("Non può essere negativo"));
    }

    @Test
    void ricercaDisponibiliEOrdinamento() throws Exception {
        crea(URL, admin, prodotto("Davines", "Shampoo nutriente", 5));
        crea(URL, admin, prodotto("Alfaparf", "Balsamo", 0));
        crea(URL, admin, prodotto("Kérastase", "Shampoo 100% naturale", 30));
        crea(URL, admin, """
                {"marca":"Wella","descrizione":"Lacca","quantita":2,"aliquotaIva":22,"calcolaDa":"IVATO",
                 "prezzoIvato":30,"prezzoVendita":35,"note":"Solo su ordinazione"}""");

        // Marca e descrizione in entrambi gli ordini, note.
        chiama(get(URL).param("q", "SHAMPOO"), admin).andExpect(jsonPath("$.totale").value(2));
        chiama(get(URL).param("q", "davines shampoo"), admin).andExpect(jsonPath("$.totale").value(1));
        chiama(get(URL).param("q", "balsamo alfaparf"), admin).andExpect(jsonPath("$.totale").value(1));
        chiama(get(URL).param("q", "ordinazione"), admin).andExpect(jsonPath("$.elementi[0].marca").value("Wella"));
        chiama(get(URL).param("q", "100%"), admin).andExpect(jsonPath("$.totale").value(1));

        // Predefinito per marca; solo disponibili esclude la quantità 0.
        chiama(get(URL), operatore)
                .andExpect(jsonPath("$.totale").value(4))
                .andExpect(jsonPath("$.elementi[0].marca").value("Alfaparf"));
        chiama(get(URL).param("disponibili", "true"), operatore)
                .andExpect(jsonPath("$.totale").value(3))
                .andExpect(jsonPath("$.elementi[*].marca", not(hasItem("Alfaparf"))));
        chiama(get(URL).param("sort", "quantita,desc"), admin)
                .andExpect(jsonPath("$.elementi[0].marca").value("Kérastase"));
        chiama(get(URL).param("sort", "prezzoVendita,desc"), admin)
                .andExpect(jsonPath("$.elementi[0].marca").value("Wella"));
        chiama(get(URL).param("sort", "note,asc"), admin).andExpect(status().isOk());
    }

    @Test
    void ancheLOperatoreEliminaEdELiminazioneELogica() throws Exception {
        long id = id(crea(URL, admin, prodotto("Davines", "Maschera", 3)));

        chiama(delete(URL + "/" + id), operatore).andExpect(status().isNoContent());
        chiama(get(URL + "/" + id), admin).andExpect(status().isNotFound());
        chiama(get(URL), admin).andExpect(jsonPath("$.elementi[*].id", not(hasItem((int) id))));

        Map<String, Object> riga = jdbc.queryForMap("select deleted_at, deleted_by from prodotto where id = ?", id);
        assertThat(riga.get("deleted_at")).isNotNull();
        assertThat(riga.get("deleted_by")).isNotNull();
    }

    @Test
    void unAltraAttivitaNonVedeIProdotti() throws Exception {
        long id = id(crea(URL, admin, prodotto("Davines", "Olio", 1)));

        chiama(get(URL + "/" + id), adminAltraAttivita).andExpect(status().isNotFound());
        invia(put(URL + "/" + id), adminAltraAttivita, prodotto("X", "Y", 1).replace("}", ",\"version\":0}"))
                .andExpect(status().isNotFound());
        chiama(delete(URL + "/" + id), adminAltraAttivita).andExpect(status().isNotFound());
        chiama(get(URL), adminAltraAttivita).andExpect(jsonPath("$.totale").value(0));
    }

    @Test
    void unaModificaBasataSuDatiVecchiVieneRifiutata() throws Exception {
        long id = id(crea(URL, admin, prodotto("Davines", "Siero", 4)));
        String v0 = prodotto("Davines", "Siero", 2).replace("}", ",\"version\":0}");
        invia(put(URL + "/" + id), admin, v0).andExpect(status().isOk());
        invia(put(URL + "/" + id), operatore, v0).andExpect(status().isConflict());
        invia(put(URL + "/" + id), operatore, prodotto("Davines", "Siero", 2)).andExpect(status().isBadRequest());
    }

    @Test
    void ilSuperadminSenzaAttivitaNonAccedeAiProdotti() throws Exception {
        chiama(get(URL), token(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD)).andExpect(status().isForbidden());
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
