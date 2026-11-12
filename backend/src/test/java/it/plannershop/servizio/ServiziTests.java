package it.plannershop.servizio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import it.plannershop.AbstractAttivitaTest;

class ServiziTests extends AbstractAttivitaTest {

    private static final String URL = "/api/servizi";

    private static String servizio(String nome, String descrizione, String prezzoVendita) {
        return """
                {"nome":"%s","descrizione":"%s","aliquotaIva":22,"calcolaDa":"IVATO","prezzoIvato":30,
                 "prezzoVendita":%s}""".formatted(nome, descrizione, prezzoVendita);
    }

    @Test
    void inserimentoModificaEdEliminazione() throws Exception {
        long id = id(invia(post(URL), operatore, """
                {"nome":" Piega ","descrizione":"Capelli corti","aliquotaIva":22,"calcolaDa":"NETTO",
                 "prezzoNetto":20,"prezzoVendita":25}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Piega"))
                .andExpect(jsonPath("$.prezzoIvato").value(24.40))
                .andExpect(jsonPath("$.quantita").doesNotExist())
                .andReturn().getResponse().getContentAsString());

        invia(put(URL + "/" + id), operatore, """
                {"nome":"Piega","descrizione":"Capelli lunghi","aliquotaIva":10,"calcolaDa":"IVATO",
                 "prezzoIvato":33,"prezzoVendita":33,"version":0}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prezzoNetto").value(30.00))
                .andExpect(jsonPath("$.aliquotaIva").value(10))
                .andExpect(jsonPath("$.version").value(1));
        invia(put(URL + "/" + id), admin, servizio("Piega", "X", "1").replace("}", ",\"version\":0}"))
                .andExpect(status().isConflict());

        chiama(delete(URL + "/" + id), operatore).andExpect(status().isNoContent());
        chiama(get(URL + "/" + id), admin).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select deleted_by from servizio where id = ?", Long.class, id)).isNotNull();
    }

    @Test
    void datiNonValidi() throws Exception {
        invia(post(URL), admin, """
                {"nome":"","aliquotaIva":-1,"calcolaDa":"IVATO","prezzoNetto":10}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.nome").value("Inserisci il nome"))
                .andExpect(jsonPath("$.errori.descrizione").value("Inserisci la descrizione"))
                .andExpect(jsonPath("$.errori.aliquotaIva").value("Aliquota non valida"))
                .andExpect(jsonPath("$.errori.prezzo").value("Inserisci il prezzo"))
                .andExpect(jsonPath("$.errori.prezzoVendita").value("Inserisci il prezzo di vendita"));
    }

    @Test
    void ricercaOrdinamentoEIsolamento() throws Exception {
        crea(URL, admin, servizio("Taglio", "Uomo", "15"));
        crea(URL, admin, servizio("Colore", "Tinta completa", "45"));
        crea(URL, admin, servizio("Piega", "Con phon", "20").replace("}", ",\"note\":\"Taglio escluso\"}"));

        chiama(get(URL).param("q", "taglio"), operatore).andExpect(jsonPath("$.totale").value(2));
        chiama(get(URL).param("q", "TINTA"), operatore).andExpect(jsonPath("$.elementi[0].nome").value("Colore"));
        chiama(get(URL), operatore).andExpect(jsonPath("$.elementi[0].nome").value("Colore"));
        chiama(get(URL).param("sort", "prezzoVendita,asc"), operatore)
                .andExpect(jsonPath("$.elementi[0].nome").value("Taglio"));

        chiama(get(URL), adminAltraAttivita).andExpect(jsonPath("$.totale").value(0));
    }
}
