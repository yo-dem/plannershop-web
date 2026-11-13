package it.plannershop.fornitore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import it.plannershop.AbstractAttivitaTest;

class FornitoriTests extends AbstractAttivitaTest {

    private static final String URL = "/api/fornitori";

    private static String fornitore(String nome) {
        return """
                {"nome":"%s","indirizzo":"Via Roma 1","telefono":"095 123456"}""".formatted(nome);
    }

    @Test
    void inserimentoConIbanNormalizzatoEModifica() throws Exception {
        long id = id(invia(post(URL), operatore, """
                {"nome":" Maxico srl ","indirizzo":"Via Postiglione 46","telefono":"011 3473989",
                 "email":"Amministrazione@Maxico.com","iban":" it60 x054 2811 1010 0000 0123 456","note":""}""")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Maxico srl"))
                .andExpect(jsonPath("$.email").value("amministrazione@maxico.com"))
                .andExpect(jsonPath("$.iban").value("IT60X0542811101000000123456"))
                .andExpect(jsonPath("$.note").doesNotExist())
                .andReturn().getResponse().getContentAsString());

        // IBAN ed email facoltativi: si possono togliere.
        invia(put(URL + "/" + id), operatore, fornitore("Maxico srl").replace("}", ",\"iban\":\"\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").doesNotExist())
                .andExpect(jsonPath("$.version").value(1));
        invia(put(URL + "/" + id), admin, fornitore("X").replace("}", ",\"version\":0}"))
                .andExpect(status().isConflict());
    }

    @Test
    void datiNonValidi() throws Exception {
        invia(post(URL), admin, """
                {"nome":" ","email":"non-valida","iban":"IT61X0542811101000000123456"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.nome").value("Inserisci il nome"))
                .andExpect(jsonPath("$.errori.indirizzo").value("Inserisci l'indirizzo"))
                .andExpect(jsonPath("$.errori.telefono").value("Inserisci il telefono"))
                .andExpect(jsonPath("$.errori.email").value("Email non valida"))
                .andExpect(jsonPath("$.errori.iban").value("IBAN non valido"));
    }

    @Test
    void ricercaEOrdinamento() throws Exception {
        crea(URL, admin, fornitore("Thecno fire"));
        crea(URL, admin, fornitore("aestetic project").replace("}", ",\"email\":\"info@aesteticproject.it\"}"));
        crea(URL, admin, fornitore("Massimo Rizzi").replace("}", ",\"iban\":\"DE89370400440532013000\"}"));

        chiama(get(URL), operatore)
                .andExpect(jsonPath("$.totale").value(3))
                .andExpect(jsonPath("$.elementi[0].nome").value("aestetic project"));
        chiama(get(URL).param("q", "AESTETICPROJECT.IT"), operatore).andExpect(jsonPath("$.totale").value(1));
        chiama(get(URL).param("q", "de8937"), operatore).andExpect(jsonPath("$.elementi[0].nome").value("Massimo Rizzi"));
        chiama(get(URL).param("q", "DE89 3704 0044"), operatore).andExpect(jsonPath("$.totale").value(1));
        chiama(get(URL).param("q", "roma"), operatore).andExpect(jsonPath("$.totale").value(3));
        chiama(get(URL).param("sort", "nome,desc"), operatore)
                .andExpect(jsonPath("$.elementi[0].nome").value("Thecno fire"));
    }

    @Test
    void eliminazioneLogicaEIsolamento() throws Exception {
        long id = id(crea(URL, admin, fornitore("Fornitore da eliminare")));

        chiama(get(URL + "/" + id), adminAltraAttivita).andExpect(status().isNotFound());
        chiama(delete(URL + "/" + id), adminAltraAttivita).andExpect(status().isNotFound());
        chiama(get(URL), adminAltraAttivita).andExpect(jsonPath("$.totale").value(0));

        chiama(delete(URL + "/" + id), operatore).andExpect(status().isNoContent());
        chiama(get(URL + "/" + id), admin).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select deleted_by from fornitore where id = ?", Long.class, id)).isNotNull();
    }
}
