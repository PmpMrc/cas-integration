package it.tivusat.cas.infrastructure.nagra.dto;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AdmBundledAccountRequest extends AdmAccountRequest {

    private final List<AdmDeviceRequest> devices;

    private AdmBundledAccountRequest(
            String accountId,
            String ua,
            String caSn
    ) {
        super(
                accountId,
                "ACTIVE",
                Collections.emptyList(),
                "MOP"
        );

        this.devices = Collections.singletonList(
                new AdmDeviceRequest(
                        accountId,
                        "DISABLED",
                        caSn,
                        ua,
                        accountId
                )
        );
    }

    public static AdmBundledAccountRequest preload(
            String accountId,
            String ua
    ) {
        return preload(accountId, ua, null);
    }

    public static AdmBundledAccountRequest preload(
            String accountId,
            String ua,
            String caSn
    ) {
        Objects.requireNonNull(accountId, "accountId is required");
        Objects.requireNonNull(ua, "ua is required");

        return new AdmBundledAccountRequest(accountId, ua, caSn);
    }

    public List<AdmDeviceRequest> getDevices() {
        return devices;
    }
}