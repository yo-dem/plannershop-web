package it.plannershop.prodotto;

import static it.plannershop.common.Testi.pulisci;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import it.plannershop.common.Prezzi.CalcolaDa;
import it.plannershop.common.RichiestaPrezzi;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProdottoDtos {

    private ProdottoDtos() {
    }

    public record ProdottoDto(
            Long id,
            LocalDate dataCarico,
            String marca,
            String descrizione,
            int quantita,
            BigDecimal aliquotaIva,
            BigDecimal prezzoNetto,
            BigDecimal prezzoIvato,
            BigDecimal prezzoVendita,
            String note,
            long version,
            Instant createdAt,
            Instant updatedAt) {

        static ProdottoDto di(Prodotto p) {
            var pr = p.getPrezzi();
            return new ProdottoDto(p.getId(), p.getDataCarico(), p.getMarca(), p.getDescrizione(), p.getQuantita(),
                    pr.getAliquotaIva(), pr.getPrezzoNetto(), pr.getPrezzoIvato(), pr.getPrezzoVendita(), p.getNote(),
                    p.getVersion(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }

    /**
     * Dati di un prodotto da creare o modificare.
     *
     * @param dataCarico in inserimento, se assente: oggi
     * @param calcolaDa  quale prezzo ha inserito l'utente (l'altro viene calcolato)
     * @param version    obbligatoria in modifica: la versione letta
     */
    public record ProdottoRequest(
            LocalDate dataCarico,
            @NotBlank(message = "Inserisci la marca") @Size(max = 100, message = "Massimo 100 caratteri") String marca,
            @NotBlank(message = "Inserisci la descrizione") @Size(max = 200, message = "Massimo 200 caratteri") String descrizione,
            @NotNull(message = "Inserisci la quantità") @Min(value = 0, message = "Non può essere negativa")
            @Max(value = 1_000_000, message = "Quantità troppo grande") Integer quantita,
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

        public ProdottoRequest {
            marca = pulisci(marca);
            descrizione = pulisci(descrizione);
            note = pulisci(note);
        }
    }
}
