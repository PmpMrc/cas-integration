package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public final class SubscriptionEntitlementFactory {

    private SubscriptionEntitlementFactory() {
    }

    public static RmgEntitlementRequest create(
            String sn,
            SmartcardType smartcardType,
            Instant activationTime
    ) {
        if (sn == null || sn.trim().isEmpty()) {
            throw new IllegalArgumentException("sn is required");
        }
        if (smartcardType == null) {
            throw new IllegalArgumentException("smartcardType is required");
        }
        if (activationTime == null) {
            throw new IllegalArgumentException("activationTime is required");
        }

        String accountId = sn + "_" + smartcardType.getNagraType();

        Instant validFrom = activationTime.truncatedTo(ChronoUnit.SECONDS);
        Instant expiryDate = validFrom
                .atZone(ZoneOffset.UTC)
                .plusYears(4)
                .toInstant();

        return RmgEntitlementRequest.subscription(
                accountId,
                accountId,
                "22",
                validFrom,
                expiryDate
        );
    }
}