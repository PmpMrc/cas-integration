package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.infrastructure.nagra.dto.NagraDeviceResponse;
import it.tivusat.cas.infrastructure.nagra.dto.NagraEntitlementResponse;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.infrastructure.nagra.dto.NagraBundledAccountResponse;
import it.tivusat.cas.infrastructure.nagra.dto.NagraBundledDeviceResponse;

public class NagraSmartcardStatusSnapshot {

    private final NagraDeviceResponse device;
    private final NagraEntitlementResponse entitlement;
    private final NagraBundledAccountResponse account;
    private final NagraBundledDeviceResponse bundledDevice;
    private final SmartcardStatus status;
    private final boolean deviceNotFound;
    private final boolean skipped;

    public NagraSmartcardStatusSnapshot(
            NagraDeviceResponse device,
            NagraEntitlementResponse entitlement,
            boolean deviceNotFound,
            boolean skipped
    ) {
        this(
                device,
                entitlement,
                deviceNotFound,
                skipped,
                null,
                null,
                null
        );
    }

    private NagraSmartcardStatusSnapshot(
            NagraDeviceResponse device,
            NagraEntitlementResponse entitlement,
            boolean deviceNotFound,
            boolean skipped,
            NagraBundledAccountResponse account,
            NagraBundledDeviceResponse bundledDevice,
            SmartcardStatus status
    ) {
        this.device = device;
        this.entitlement = entitlement;
        this.deviceNotFound = deviceNotFound;
        this.skipped = skipped;
        this.account = account;
        this.bundledDevice = bundledDevice;
        this.status = status;
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

    public static NagraSmartcardStatusSnapshot bundled(
            NagraBundledAccountResponse account,
            NagraBundledDeviceResponse device,
            NagraEntitlementResponse entitlement,
            SmartcardStatus status
    ) {
        return new NagraSmartcardStatusSnapshot(
                null,
                entitlement,
                false,
                false,
                account,
                device,
                status
        );
    }

    public NagraBundledAccountResponse account() { return account; }

    public NagraBundledDeviceResponse bundledDevice() { return bundledDevice; }

    public SmartcardStatus status() { return status; }

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