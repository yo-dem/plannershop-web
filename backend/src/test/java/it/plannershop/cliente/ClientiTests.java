package it.plannershop.cliente;

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

class ClientiTests extends AbstractAttivitaTest {

    private static String cliente(String nome, String cognome) {
        return "{\"nome\":\"%s\",\"cognome\":\"%s\"}".formatted(nome, cognome);
    }

    /** Crea un cliente e restituisce il suo JSON. */
    private String crea(String token, String json) throws Exception {
        return crea("/api/clienti", token, json);
    }

    // ---------------------------------------------------------------- test

    @Test
    void inserimentoLetturaEModifica() throws Exception {
        String creato = invia(post("/api/clienti"), operatore, """
                {"nome":"  Mario ","cognome":"Rossi","giornoNascita":29,"meseNascita":2,
                 "telefono":" 333 1234567 ","email":" Mario.Rossi@Esempio.IT","indirizzo":"","note":"Cliente storico"}""")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Mario"))
                .andExpect(jsonPath("$.telefono").value("333 1234567"))
                .andExpect(jsonPath("$.email").value("mario.rossi@esempio.it"))
                .andExpect(jsonPath("$.indirizzo").doesNotExist())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();
        long id = id(creato);

        chiama(get("/api/clienti/" + id), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.giornoNascita").value(29))
                .andExpect(jsonPath("$.meseNascita").value(2));

        invia(put("/api/clienti/" + id), operatore, """
                {"nome":"Mario","cognome":"Rossi Bianchi","version":0}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cognome").value("Rossi Bianchi"))
                .andExpect(jsonPath("$.giornoNascita").doesNotExist())
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void datiNonValidiRestituisconoGliErroriPerCampo() throws Exception {
        invia(post("/api/clienti"), admin, """
                {"nome":" ","email":"non-una-email","giornoNascita":31,"meseNascita":2}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.nome").value("Inserisci il nome"))
                .andExpect(jsonPath("$.errori.cognome").value("Inserisci il cognome"))
                .andExpect(jsonPath("$.errori.email").value("Email non valida"))
                .andExpect(jsonPath("$.errori.compleanno").exists());

        invia(post("/api/clienti"), admin, """
                {"nome":"A","cognome":"B","giornoNascita":5}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.compleanno").exists());
    }

    @Test
    void ricercaPaginazioneEOrdinamento() throws Exception {
        crea(admin, cliente("Mario", "Rossi"));
        crea(admin, cliente("Anna", "Verdi"));
        crea(admin, cliente("Luca", "Bianchi"));
        crea(admin, """
                {"nome":"Giulia","cognome":"Neri","telefono":"347 9876543","email":"giulia@esempio.it"}""");
        crea(admin, cliente("Paolo", "Rossini"));

        // Nome e cognome in entrambi gli ordini, maiuscole indifferenti.
        chiama(get("/api/clienti").param("q", "MARIO rossi"), admin)
                .andExpect(jsonPath("$.totale").value(1))
                .andExpect(jsonPath("$.elementi[0].cognome").value("Rossi"));
        chiama(get("/api/clienti").param("q", "rossi mario"), admin).andExpect(jsonPath("$.totale").value(1));
        chiama(get("/api/clienti").param("q", "ross"), admin).andExpect(jsonPath("$.totale").value(2));
        // Telefono ed email.
        chiama(get("/api/clienti").param("q", "9876"), admin).andExpect(jsonPath("$.elementi[0].nome").value("Giulia"));
        chiama(get("/api/clienti").param("q", "giulia@"), admin).andExpect(jsonPath("$.totale").value(1));
        // I caratteri jolly di LIKE vengono cercati come testo.
        chiama(get("/api/clienti").param("q", "%"), admin).andExpect(jsonPath("$.totale").value(0));

        // Predefinito: per cognome.
        chiama(get("/api/clienti").param("size", "2"), admin)
                .andExpect(jsonPath("$.totale").value(5))
                .andExpect(jsonPath("$.dimensione").value(2))
                .andExpect(jsonPath("$.elementi.length()").value(2))
                .andExpect(jsonPath("$.elementi[0].cognome").value("Bianchi"));
        chiama(get("/api/clienti").param("page", "2").param("size", "2"), admin)
                .andExpect(jsonPath("$.elementi.length()").value(1))
                .andExpect(jsonPath("$.elementi[0].cognome").value("Verdi"));
        chiama(get("/api/clienti").param("sort", "nome,desc"), admin)
                .andExpect(jsonPath("$.elementi[0].nome").value("Paolo"));
        // Un ordinamento non previsto viene ignorato, senza errori.
        chiama(get("/api/clienti").param("sort", "passwordHash,asc"), admin).andExpect(status().isOk());
    }

    @Test
    void soloLAdminEliminaEdELiminazioneELogica() throws Exception {
        long id = id(crea(operatore, cliente("Elena", "Gialli")));

        chiama(delete("/api/clienti/" + id), operatore).andExpect(status().isForbidden());
        chiama(delete("/api/clienti/" + id), admin).andExpect(status().isNoContent());

        chiama(get("/api/clienti/" + id), admin).andExpect(status().isNotFound());
        chiama(get("/api/clienti"), admin)
                .andExpect(jsonPath("$.elementi[*].id", not(hasItem((int) id))));

        Map<String, Object> riga = jdbc.queryForMap("select deleted_at, deleted_by from cliente where id = ?", id);
        assertThat(riga.get("deleted_at")).isNotNull();
        assertThat(riga.get("deleted_by")).isNotNull();
    }

    @Test
    void unAltraAttivitaNonVedeNeModificaIClienti() throws Exception {
        long id = id(crea(admin, cliente("Sara", "Blu")));

        chiama(get("/api/clienti/" + id), adminAltraAttivita).andExpect(status().isNotFound());
        invia(put("/api/clienti/" + id), adminAltraAttivita, """
                {"nome":"X","cognome":"Y","version":0}""").andExpect(status().isNotFound());
        chiama(delete("/api/clienti/" + id), adminAltraAttivita).andExpect(status().isNotFound());
        chiama(get("/api/clienti"), adminAltraAttivita).andExpect(jsonPath("$.totale").value(0));
    }

    @Test
    void unaModificaBasataSuDatiVecchiVieneRifiutata() throws Exception {
        long id = id(crea(admin, cliente("Marco", "Viola")));
        invia(put("/api/clienti/" + id), admin, "{\"nome\":\"Marco\",\"cognome\":\"Viola\",\"telefono\":\"123\",\"version\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        invia(put("/api/clienti/" + id), operatore, "{\"nome\":\"Marco\",\"cognome\":\"Rosa\",\"version\":0}")
                .andExpect(status().isConflict());
        invia(put("/api/clienti/" + id), operatore, "{\"nome\":\"Marco\",\"cognome\":\"Rosa\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void compleanniDelMeseOrdinatiPerGiorno() throws Exception {
        crea(admin, "{\"nome\":\"C\",\"cognome\":\"Tre\",\"giornoNascita\":20,\"meseNascita\":3}");
        crea(admin, "{\"nome\":\"A\",\"cognome\":\"Uno\",\"giornoNascita\":2,\"meseNascita\":3}");
        crea(admin, "{\"nome\":\"B\",\"cognome\":\"Due\",\"giornoNascita\":2,\"meseNascita\":4}");
        crea(admin, cliente("Senza", "Compleanno"));

        chiama(get("/api/clienti/compleanni").param("mese", "3"), operatore)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].cognome").value("Uno"))
                .andExpect(jsonPath("$[1].cognome").value("Tre"));
        chiama(get("/api/clienti/compleanni").param("mese", "13"), admin).andExpect(status().isBadRequest());

        // Senza mese: il mese corrente nel fuso orario dell'attività.
        int meseCorrente = LocalDate.now(ZoneId.of("Europe/Rome")).getMonthValue();
        crea(admin, "{\"nome\":\"D\",\"cognome\":\"Oggi\",\"giornoNascita\":1,\"meseNascita\":%d}".formatted(meseCorrente));
        chiama(get("/api/clienti/compleanni"), admin)
                .andExpect(jsonPath("$[*].cognome", hasItem("Oggi")));
    }

    @Test
    void ilSuperadminSenzaAttivitaNonAccedeAiClienti() throws Exception {
        chiama(get("/api/clienti"), token(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD)).andExpect(status().isForbidden());
        mvc.perform(get("/api/clienti")).andExpect(status().isUnauthorized());
    }
}
