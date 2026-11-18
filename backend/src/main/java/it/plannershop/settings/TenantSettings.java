package it.plannershop.settings;

import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.TenantScopedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Settings of a tenant, one row per tenant. See SPECIFICA §6.9. */
@Entity
@Table(name = "tenant_settings")
@SQLRestriction("deleted_at IS NULL")
public class TenantSettings extends TenantScopedEntity {

    /** Default values, also used while the tenant has never saved its settings. */
    public static final int DEFAULT_OUT_OF_STOCK_THRESHOLD = 0;
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    /** Products with quantity at or below this value are shown in red. */
    private int outOfStockThreshold = DEFAULT_OUT_OF_STOCK_THRESHOLD;
    /** Products with quantity at or below this value (and above the red one) are shown in orange. */
    private int lowStockThreshold = DEFAULT_LOW_STOCK_THRESHOLD;

    public int getOutOfStockThreshold() {
        return outOfStockThreshold;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setStockThresholds(int outOfStock, int lowStock) {
        this.outOfStockThreshold = outOfStock;
        this.lowStockThreshold = lowStock;
    }
}
