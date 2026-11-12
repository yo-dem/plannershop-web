package it.plannershop.servizio;

import static it.plannershop.common.Testi.pulisci;

import java.math.BigDecimal;
import java.time.Instant;

import it.plannershop.common.Prezzi.CalcolaDa;
import it.plannershop.common.RichiestaPrezzi;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ServizioDtos {

    private ServizioDtos() {
    }

    public record ServizioDto(
            Long id,
            String nome,
            String descrizione,
            BigDecimal aliquotaIva,
            BigDecimal prezzoNetto,
            BigDecimal prezzoIvato,
            BigDecimal prezzoVendita,
            String note,
            long version,
            Instant createdAt,
            Instant updatedAt) {

        static ServizioDto di(Servizio s) {
            var pr = s.getPrezzi();
            return new ServizioDto(s.getId(), s.getNome(), s.getDescrizione(), pr.getAliquotaIva(),
                    pr.getPrezzoNetto(), pr.getPrezzoIvato(), pr.getPrezzoVendita(), s.getNote(), s.getVersion(),
                    s.getCreatedAt(), s.getUpdatedAt());
        }
    }

    /**
     * Dati di un servizio da creare o modificare.
     *
     * @param calcolaDa quale prezzo ha inserito l'utente (l'altro viene calcolato)
     * @param version   obbligatoria in modifica: la versione letta
     */
    public record ServizioRequest(
            @NotBlank(message = "Inserisci il nome") @Size(max = 100, message = "Massimo 100 caratteri") String nome,
            @NotBlank(message = "Inserisci la descrizione") @Size(max = 200, message = "Massimo 200 caratteri") String descrizione,
            @NotNull(message = "Inserisci l'aliquota") @DecimalMin(value = "0", message = "Aliquota non valida")
            @DecimalMax(value = "100", message = "Aliquota non valida")
            @Digits(integer = 3, fraction = 2, message = "Massimo due decimali") BigDecimal aliquotaIva,
            @NotNull(message = "Indica quale prezzo hai inserito") CalcolaDa calcolaDa,
            @DecimalMin(value = "0", message = "Non può essere negativo")
            @Digits(integer = 7, fraction = 2, message = "Importo non valido") BigDecimal prezzoNetto,
            @DecimalMin(value = "0", message = "Non può essere negativo")
            @Digits(integer = 7, fraction = 2, message = "Importo non valido") BigDecimal prezzoIvato,
            @NotNull(message = "Inserisci il prezzo di vendita") @DecimalMin(value = "0", message = "Non può essere negativo")
            @Digits(integer = 7, fraction = 2, message = "Importo non valido") BigDecimal prezzoVendita,
            @Size(max = 4000, message = "Massimo 4000 caratteri") String note,
            Long version) implements RichiestaPrezzi {

        public ServizioRequest {
            nome = pulisci(nome);
            descrizione = pulisci(descrizione);
            note = pulisci(note);
        }
    }
}
