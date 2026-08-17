package it.tivusat.cas.infrastructure.nagra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record RmgEntitlementRequest(
        @JsonProperty("_id")
        String id,

        String accountId,
        String productId,
        String status,
        String validityType,
        String productType,
        Instant validityFrom,
        Instant expiryDate
) {

    public static RmgEntitlementRequest subscription(
            String type,
            String sn,
            String productId,
            Instant validityFrom,
            Instant expiryDate
    ) {
        return new RmgEntitlementRequest(
                type,
                sn,
                productId,
                "SUBSCRIBED",
                "ABSOLUTE",
                "SUBSCRIPTION",
                validityFrom,
                expiryDate
        );
    }
}