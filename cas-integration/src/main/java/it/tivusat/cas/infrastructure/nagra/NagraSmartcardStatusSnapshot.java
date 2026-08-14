package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.infrastructure.nagra.dto.NagraDeviceResponse;
import it.tivusat.cas.infrastructure.nagra.dto.NagraEntitlementResponse;

public record NagraSmartcardStatusSnapshot(
        NagraDeviceResponse device,
        NagraEntitlementResponse entitlement,
        boolean deviceNotFound,
        boolean skipped
) {

    public static NagraSmartcardStatusSnapshot of(
            NagraDeviceResponse device,
            NagraEntitlementResponse entitlement
    ) {
        return new NagraSmartcardStatusSnapshot(
                device,
                entitlement,
                false,
                false
        );
    }

    public static NagraSmartcardStatusSnapshot deviceNotFound(
            NagraEntitlementResponse entitlement
    ) {
        return new NagraSmartcardStatusSnapshot(
                null,
                entitlement,
                true,
                false
        );
    }

    public static NagraSmartcardStatusSnapshot integrationDisabled() {
        return new NagraSmartcardStatusSnapshot(
                null,
                null,
                false,
                true
        );
    }
}