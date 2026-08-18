package it.tivusat.cas.api.dto;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.SmartcardType;

import java.time.Instant;

public class SmartcardResponse {

    private String sn;
    private String ua;
    private SmartcardType smartcardType;
    private SmartcardSource source;
    private SmartcardStatus status;
    private boolean accountCreated;
    private boolean deviceCreated;
    private String entitlementId;
    private String productId;
    private String caSn;
    private Instant expiryDate;
    private Instant createdAt;
    private Instant updatedAt;

    public SmartcardResponse() {
    }

    public SmartcardResponse(
            String sn,
            String ua,
            SmartcardType smartcardType,
            SmartcardSource source,
            SmartcardStatus status,
            boolean accountCreated,
            boolean deviceCreated,
            String entitlementId,
            String productId,
            String caSn,
            Instant expiryDate,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.sn = sn;
        this.ua = ua;
        this.smartcardType = smartcardType;
        this.source = source;
        this.status = status;
        this.accountCreated = accountCreated;
        this.deviceCreated = deviceCreated;
        this.entitlementId = entitlementId;
        this.productId = productId;
        this.caSn = caSn;
        this.expiryDate = expiryDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
}