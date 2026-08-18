package it.tivusat.cas.infrastructure.nagra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class RmgEntitlementRequest {

    @JsonProperty("_id")
    private String id;

    private String accountId;
    private String productId;
    private String status;
    private String validityType;
    private String productType;
    private Instant validityFrom;
    private Instant expiryDate;

    public RmgEntitlementRequest() {
    }

    public RmgEntitlementRequest(
            String id,
            String accountId,
            String productId,
            String status,
            String validityType,
            String productType,
            Instant validityFrom,
            Instant expiryDate
    ) {
        this.id = id;
        this.accountId = accountId;
        this.productId = productId;
        this.status = status;
        this.validityType = validityType;
        this.productType = productType;
        this.validityFrom = validityFrom;
        this.expiryDate = expiryDate;
    }

    public static RmgEntitlementRequest subscription(
            String entitlementId,
            String accountId,
            String productId,
            Instant validityFrom,
            Instant expiryDate
    ) {
        return new RmgEntitlementRequest(
                entitlementId,
                accountId,
                productId,
                "SUBSCRIBED",
                "ABSOLUTE",
                "SUBSCRIPTION",
                validityFrom,
                expiryDate
        );
    }

    public String getId() {
        return id;
    }

    public String id() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public String accountId() {
        return accountId;
    }

    public String getProductId() {
        return productId;
    }

    public String productId() {
        return productId;
    }

    public String getStatus() {
        return status;
    }

    public String status() {
        return status;
    }

    public String getValidityType() {
        return validityType;
    }

    public String validityType() {
        return validityType;
    }

    public String getProductType() {
        return productType;
    }

    public String productType() {
        return productType;
    }

    public Instant getValidityFrom() {
        return validityFrom;
    }

    public Instant validityFrom() {
        return validityFrom;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public Instant expiryDate() {
        return expiryDate;
    }
}