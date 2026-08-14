package it.tivusat.cas.infrastructure.nagra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record NagraEntitlementResponse(
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
}