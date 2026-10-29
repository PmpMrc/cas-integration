package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class NagraSmartcardGateway {

    private static final Logger log = LoggerFactory.getLogger(NagraSmartcardGateway.class);

    private final NagraProperties properties;
    private final NagraAdmClient nagraAdmClient;
    private final NagraRmgClient nagraRmgClient;

    public NagraSmartcardGateway(
            NagraProperties properties,
            NagraAdmClient nagraAdmClient,
            NagraRmgClient nagraRmgClient
    ) {
        this.properties = properties;
        this.nagraAdmClient = nagraAdmClient;
        this.nagraRmgClient = nagraRmgClient;
    }

    public void preloadSmartcard(
            String sn,
            String ua,
            SmartcardType smartcardType,
            SmartcardSource source
    ) {
        if (!properties.enabled()) {
            throw new NagraException(
                    null,
                    "NAGRA integration disabled; preload was not executed",
                    null,
                    NagraOperation.ADM_CREATE_BUNDLED_ACCOUNT,
                    properties.paths().createBundledAccount(),
                    NagraException.FailureType.INTEGRATION_DISABLED
            );
        }

        String accountId = sn + "_" + smartcardType.getNagraType();

        AdmBundledAccountRequest request =
                AdmBundledAccountRequest.preload(accountId, ua);

        nagraAdmClient.createBundledAccount(source, sn, request);
    }

    public void activateSmartcard(
            String sn,
            String ua,
            SmartcardSource source,
            String caSn
    ) {
        if (!properties.enabled()) {
            log.warn(
                    "NAGRA integration disabled. Skipping activation for smartcard {}",
                    sn
            );
            return;
        }

        nagraAdmClient.createDevice(
                source,
                AdmDeviceRequest.activate(sn, ua, caSn)
        );
    }

    public void refreshSmartcard(String sn, String ua, SmartcardSource source, String caSn) {
        if (!properties.enabled()) {
            log.warn("NAGRA integration disabled. Skipping refresh for smartcard {}", sn);
            return;
        }

        nagraAdmClient.updateDeviceForRefresh(
                source,
                sn,
                AdmDeviceRequest.refresh(sn, ua, caSn)
        );
    }

    public void suspendSmartcard(String sn, String ua, SmartcardSource source) {
        if (!properties.enabled()) {
            log.warn("NAGRA integration disabled. Skipping suspend for smartcard {}", sn);
            return;
        }

        nagraAdmClient.suspendDevice(source, sn);
    }

    public void deleteSmartcard(String sn, SmartcardSource source) {
        if (!properties.enabled()) {
            log.warn("NAGRA integration disabled. Skipping delete for smartcard {}", sn);
            return;
        }

        nagraAdmClient.deleteDevice(source, sn);
    }

    public NagraSmartcardStatusSnapshot fetchSmartcardStatus(String sn, SmartcardSource source) {
        if (!properties.enabled()) {
            log.warn("NAGRA integration disabled. Skipping status fetch for smartcard {}", sn);
            return NagraSmartcardStatusSnapshot.integrationDisabled();
        }

        NagraEntitlementResponse entitlement =
                nagraRmgClient.getEntitlementsByAccountId(source, sn);

        try {
            NagraDeviceResponse device = nagraAdmClient.getDevice(source, sn);
            return NagraSmartcardStatusSnapshot.of(device, entitlement);
        } catch (NagraException exception) {
            if (exception.isDeviceNotFound()) {
                return NagraSmartcardStatusSnapshot.deviceNotFound(entitlement);
            }
            throw exception;
        }
    }
}
