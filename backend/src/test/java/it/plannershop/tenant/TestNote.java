package it.plannershop.tenant;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.TenantScopedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Test-only entity owned by a tenant. */
@Entity
@Table(name = "test_note")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE test_note SET deleted_at = now() WHERE id = ? AND version = ?")
public class TestNote extends TenantScopedEntity {

    private String content;

    protected TestNote() {
    }

    public TestNote(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
