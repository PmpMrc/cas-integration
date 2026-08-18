package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.infrastructure.nagra.dto.NagraDeviceResponse;
import it.tivusat.cas.infrastructure.nagra.dto.NagraEntitlementResponse;

public class NagraSmartcardStatusSnapshot {

    private final NagraDeviceResponse device;
    private final NagraEntitlementResponse entitlement;
    private final boolean deviceNotFound;
    private final boolean skipped;

    public NagraSmartcardStatusSnapshot(
            NagraDeviceResponse device,
            NagraEntitlementResponse entitlement,
            boolean deviceNotFound,
            boolean skipped
    ) {
        this.device = device;
        this.entitlement = entitlement;
        this.deviceNotFound = deviceNotFound;
        this.skipped = skipped;
    }

    public static NagraSmartcardStatusSnapshot of(
            NagraDeviceResponse device,
            NagraEntitlementResponse entitlement
    ) {
        return new NagraSmartcardStatusSnapshot(device, entitlement, false, false);
    }

    public static NagraSmartcardStatusSnapshot deviceNotFound(
            NagraEntitlementResponse entitlement
    ) {
        return new NagraSmartcardStatusSnapshot(null, entitlement, true, false);
    }

    public static NagraSmartcardStatusSnapshot integrationDisabled() {
        return new NagraSmartcardStatusSnapshot(null, null, false, true);
    }

    public NagraDeviceResponse getDevice() {
        return device;
    }

    public NagraDeviceResponse device() {
        return device;
    }

    public NagraEntitlementResponse getEntitlement() {
        return entitlement;
    }

    public NagraEntitlementResponse entitlement() {
        return entitlement;
    }

    public boolean isDeviceNotFound() {
        return deviceNotFound;
    }

    public boolean deviceNotFound() {
        return deviceNotFound;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public boolean skipped() {
        return skipped;
    }
}