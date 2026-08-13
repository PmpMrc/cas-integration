package it.tivusat.cas.infrastructure.nagra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nagra")
public record NagraProperties(
        boolean enabled,
        String baseUrl,
        SourceId sourceId,
        Paths paths
) {

    public record SourceId(
            String physical,
            String virtual
    ) {
    }

    public record Paths(
            String createAccount,
            String createEntitlement,
            String createDevice,
            String updateDevice,
            String getDevice,
            String getEntitlements
    ) {
    }
}