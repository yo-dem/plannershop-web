package it.plannershop.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import it.plannershop.AbstractIntegrationTest;

/**
 * Verifica il cuore del multi-tenant: un'attività non vede mai i dati di un'altra,
 * e senza tenant non si vede nulla.
 */
class TenantIsolationTests extends AbstractIntegrationTest {

    @Autowired
    TenantRepository tenants;
    @Autowired
    NotaProvaRepository note;

    Long a;
    Long b;

    @BeforeEach
    void creaDueAttivita() {
        a = tenants.save(new Tenant("Attività A")).getId();
        b = tenants.save(new Tenant("Attività B")).getId();
    }

    @Test
    void ilTenantVieneImpostatoDaSoloInInserimento() {
        NotaProva n = TenantContext.callAs(a, () -> note.save(new NotaProva("nota di A")));
        assertThat(n.getTenantId()).isEqualTo(a);
    }

    @Test
    void unTenantNonVedeIDatiDiUnAltro() {
        Long id = TenantContext.callAs(a, () -> note.save(new NotaProva("segreto-A-" + a)).getId());

        TenantContext.runAs(b, () -> {
            assertThat(note.findAll()).extracting(NotaProva::getTesto).doesNotContain("segreto-A-" + a);
            assertThat(note.cercaPerTesto("segreto-A-%")).isEmpty();
            assertThat(note.findById(id)).isEmpty();
        });
        TenantContext.runAs(a, () -> assertThat(note.findById(id)).isPresent());
    }

    @Test
    void senzaTenantNonSiVedeNullaENonSiInserisce() {
        TenantContext.runAs(a, () -> note.save(new NotaProva("visibile solo ad A")));

        assertThat(note.findAll()).isEmpty();
        assertThatThrownBy(() -> note.saveAndFlush(new NotaProva("orfana")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void lEliminazioneELogica() {
        Long id = TenantContext.callAs(a, () -> note.save(new NotaProva("da eliminare")).getId());

        TenantContext.runAs(a, () -> {
            NotaProva n = note.findById(id).orElseThrow();
            n.elimina(42L);
            note.save(n);
            assertThat(note.findById(id)).isEmpty();
        });
        assertThat(note.contaRigheFisiche(id)).isEqualTo(1);
    }

    @Test
    void ancheDeleteDelRepositoryELogico() {
        Long id = TenantContext.callAs(a, () -> note.save(new NotaProva("delete accidentale")).getId());

        TenantContext.runAs(a, () -> {
            note.deleteById(id);
            assertThat(note.findById(id)).isEmpty();
        });
        assertThat(note.contaRigheFisiche(id)).isEqualTo(1);
    }
}
