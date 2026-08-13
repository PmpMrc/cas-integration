package it.tivusat.cas.infrastructure.persistence;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.SmartcardType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "smartcard")
public class SmartcardEntity {

    @Id
    private String sn;

    private String ua;

    @Enumerated(EnumType.STRING)
    private SmartcardType smartcardType;

    @Enumerated(EnumType.STRING)
    private SmartcardSource source;

    @Enumerated(EnumType.STRING)
    private SmartcardStatus status;

    private boolean accountCreated;

    private boolean deviceCreated;

    private String entitlementId;

    private String productId;

    private String caSn;

    private Instant expiryDate;

    private Instant lastSyncAt;

    private Instant createdAt;

    private Instant updatedAt;

    protected SmartcardEntity() {
    }

    public SmartcardEntity(String sn, String ua, SmartcardType smartcardType, SmartcardSource source) {
        this.sn = sn;
        this.ua = ua;
        this.smartcardType = smartcardType;
        this.source = source;
        this.status = SmartcardStatus.NEW;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getSn() {
        return sn;
    }

    public String getUa() {
        return ua;
    }

    public SmartcardType getSmartcardType() {
        return smartcardType;
    }

    public SmartcardSource getSource() {
        return source;
    }

    public SmartcardStatus getStatus() {
        return status;
    }

    public boolean isAccountCreated() {
        return accountCreated;
    }

    public boolean isDeviceCreated() {
        return deviceCreated;
    }

    public String getEntitlementId() {
        return entitlementId;
    }

    public String getProductId() {
        return productId;
    }

    public String getCaSn() {
        return caSn;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markPreloaded(String entitlementId, String productId, Instant expiryDate) {
        this.accountCreated = true;
        this.entitlementId = entitlementId;
        this.productId = productId;
        this.expiryDate = expiryDate;
        this.status = SmartcardStatus.PRELOADED;
        this.updatedAt = Instant.now();
    }

    public void markEnabled(String caSn) {
        this.status = SmartcardStatus.ENABLED;
        this.deviceCreated = true;
        this.caSn = caSn;
        this.updatedAt = Instant.now();
    }

    public void markDisabled() {
        this.status = SmartcardStatus.DISABLED;
        this.updatedAt = Instant.now();
    }

    public void markDeleted() {
        this.status = SmartcardStatus.DELETED;
        this.updatedAt = Instant.now();
    }

    public void markRefreshed(String caSn) {
        this.status = SmartcardStatus.ENABLED;
        this.caSn = caSn;
        this.updatedAt = Instant.now();
    }
}