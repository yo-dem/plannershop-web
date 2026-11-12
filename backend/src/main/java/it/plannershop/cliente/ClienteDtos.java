package it.plannershop.cliente;

import java.time.Instant;
import java.time.Month;

import static it.plannershop.common.Testi.pulisci;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ClienteDtos {

    private ClienteDtos() {
    }

    public record ClienteDto(
            Long id,
            String nome,
            String cognome,
            Short giornoNascita,
            Short meseNascita,
            String telefono,
            String email,
            String indirizzo,
            String note,
            long version,
            Instant createdAt,
            Instant updatedAt) {

        static ClienteDto di(Cliente c) {
            return new ClienteDto(c.getId(), c.getNome(), c.getCognome(), c.getGiornoNascita(), c.getMeseNascita(),
                    c.getTelefono(), c.getEmail(), c.getIndirizzo(), c.getNote(), c.getVersion(), c.getCreatedAt(),
                    c.getUpdatedAt());
        }
    }

    /**
     * Dati di un cliente da creare o modificare.
     *
     * @param version obbligatoria in modifica: la versione letta, per non
     *                sovrascrivere le modifiche fatte nel frattempo da altri
     */
    public record ClienteRequest(
            @NotBlank(message = "Inserisci il nome") @Size(max = 100, message = "Massimo 100 caratteri") String nome,
            @NotBlank(message = "Inserisci il cognome") @Size(max = 100, message = "Massimo 100 caratteri") String cognome,
            @Min(value = 1, message = "Giorno non valido") @Max(value = 31, message = "Giorno non valido") Short giornoNascita,
            @Min(value = 1, message = "Mese non valido") @Max(value = 12, message = "Mese non valido") Short meseNascita,
            @Size(max = 50, message = "Massimo 50 caratteri") String telefono,
            @Email(message = "Email non valida") @Size(max = 254, message = "Email troppo lunga") String email,
            @Size(max = 300, message = "Massimo 300 caratteri") String indirizzo,
            @Size(max = 4000, message = "Massimo 4000 caratteri") String note,
            Long version) {

        /** Spazi iniziali e finali tolti prima della validazione; testo vuoto = campo assente. */
        public ClienteRequest {
            nome = pulisci(nome);
            cognome = pulisci(cognome);
            telefono = pulisci(telefono);
            email = pulisci(email);
            indirizzo = pulisci(indirizzo);
            note = pulisci(note);
        }

        /** Giorno e mese insieme, e il giorno deve esistere in quel mese (29 febbraio ammesso). */
        @AssertTrue(message = "Indica sia il giorno sia il mese, con una data esistente")
        public boolean isCompleanno() {
            if (giornoNascita == null && meseNascita == null) {
                return true;
            }
            if (giornoNascita == null || meseNascita == null || meseNascita < 1 || meseNascita > 12) {
                return false;
            }
            return Month.of(meseNascita).maxLength() >= giornoNascita;
        }
    }
}
