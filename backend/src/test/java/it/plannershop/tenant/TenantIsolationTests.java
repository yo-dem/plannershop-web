package it.plannershop.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import it.plannershop.AbstractIntegrationTest;

/**
 * Verifies the core of multi-tenancy: a tenant never sees another tenant's data,
 * and without a tenant nothing is visible.
 */
class TenantIsolationTests extends AbstractIntegrationTest {

    @Autowired
    TenantRepository tenants;
    @Autowired
    TestNoteRepository notes;

    Long a;
    Long b;

    @BeforeEach
    void createTwoTenants() {
        a = tenants.save(new Tenant("Tenant A")).getId();
        b = tenants.save(new Tenant("Tenant B")).getId();
    }

    @Test
    void theTenantIsSetAutomaticallyOnInsert() {
        TestNote n = TenantContext.callAs(a, () -> notes.save(new TestNote("note of A")));
        assertThat(n.getTenantId()).isEqualTo(a);
    }

    @Test
    void aTenantDoesNotSeeAnotherTenantsData() {
        Long id = TenantContext.callAs(a, () -> notes.save(new TestNote("secret-A-" + a)).getId());

        TenantContext.runAs(b, () -> {
            assertThat(notes.findAll()).extracting(TestNote::getContent).doesNotContain("secret-A-" + a);
            assertThat(notes.findByContentLike("secret-A-%")).isEmpty();
            assertThat(notes.findById(id)).isEmpty();
        });
        TenantContext.runAs(a, () -> assertThat(notes.findById(id)).isPresent());
    }

    @Test
    void withoutATenantNothingIsVisibleAndNothingCanBeInserted() {
        TenantContext.runAs(a, () -> notes.save(new TestNote("visible only to A")));

        assertThat(notes.findAll()).isEmpty();
        assertThatThrownBy(() -> notes.saveAndFlush(new TestNote("orphan")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void deletionIsSoft() {
        Long id = TenantContext.callAs(a, () -> notes.save(new TestNote("to delete")).getId());

        TenantContext.runAs(a, () -> {
            TestNote n = notes.findById(id).orElseThrow();
            n.delete(42L);
            notes.save(n);
            assertThat(notes.findById(id)).isEmpty();
        });
        assertThat(notes.countPhysicalRows(id)).isEqualTo(1);
    }

    @Test
    void evenRepositoryDeleteIsSoft() {
        Long id = TenantContext.callAs(a, () -> notes.save(new TestNote("accidental delete")).getId());

        TenantContext.runAs(a, () -> {
            notes.deleteById(id);
            assertThat(notes.findById(id)).isEmpty();
        });
        assertThat(notes.countPhysicalRows(id)).isEqualTo(1);
    }
}
