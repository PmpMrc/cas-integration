package it.tivusat.cas.api.dto;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.persistence.SmartcardEntity;

import java.time.Instant;

public record SmartcardResponse(
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

    public static SmartcardResponse fromEntity(SmartcardEntity entity) {
        return new SmartcardResponse(
                entity.getSn(),
                entity.getUa(),
                entity.getSmartcardType(),
                entity.getSource(),
                entity.getStatus(),
                entity.isAccountCreated(),
                entity.isDeviceCreated(),
                entity.getEntitlementId(),
                entity.getProductId(),
                entity.getCaSn(),
                entity.getExpiryDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}