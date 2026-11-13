package it.plannershop.fornitore;

import static it.plannershop.common.Testi.pulisci;

import java.time.Instant;

import it.plannershop.common.CodiceIban;
import it.plannershop.common.Iban;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class FornitoreDtos {

    private FornitoreDtos() {
    }

    public record FornitoreDto(
            Long id,
            String nome,
            String indirizzo,
            String telefono,
            String email,
            String iban,
            String note,
            long version,
            Instant createdAt,
            Instant updatedAt) {

        static FornitoreDto di(Fornitore f) {
            return new FornitoreDto(f.getId(), f.getNome(), f.getIndirizzo(), f.getTelefono(), f.getEmail(),
                    f.getIban(), f.getNote(), f.getVersion(), f.getCreatedAt(), f.getUpdatedAt());
        }
    }

    /**
     * Dati di un fornitore da creare o modificare.
     *
     * @param iban    accettato con spazi e minuscole: viene normalizzato prima della verifica
     * @param version obbligatoria in modifica: la versione letta
     */
    public record FornitoreRequest(
            @NotBlank(message = "Inserisci il nome") @Size(max = 150, message = "Massimo 150 caratteri") String nome,
            @NotBlank(message = "Inserisci l'indirizzo") @Size(max = 300, message = "Massimo 300 caratteri") String indirizzo,
            @NotBlank(message = "Inserisci il telefono") @Size(max = 50, message = "Massimo 50 caratteri") String telefono,
            @Email(message = "Email non valida") @Size(max = 254, message = "Email troppo lunga") String email,
            @Iban String iban,
            @Size(max = 4000, message = "Massimo 4000 caratteri") String note,
            Long version) {

        public FornitoreRequest {
            nome = pulisci(nome);
            indirizzo = pulisci(indirizzo);
            telefono = pulisci(telefono);
            email = pulisci(email);
            iban = CodiceIban.normalizza(iban);
            note = pulisci(note);
        }
    }
}
