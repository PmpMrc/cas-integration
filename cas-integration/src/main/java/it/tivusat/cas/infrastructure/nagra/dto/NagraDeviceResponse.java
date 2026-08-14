package it.tivusat.cas.infrastructure.nagra.dto;

public record NagraDeviceResponse(
        String status,
        String caSN,
        String smartCardId,
        String accountUid
) {
}