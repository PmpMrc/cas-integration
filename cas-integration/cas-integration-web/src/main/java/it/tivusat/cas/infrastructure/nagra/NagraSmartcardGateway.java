package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
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
            SmartcardSource source,
            RmgEntitlementRequest entitlement
    ) {
        if (!properties.enabled()) {
            throw new NagraException(
                    null,
                    "NAGRA integration disabled; activation was not executed",
                    null,
                    NagraOperation.ADM_ACTIVATE_DEVICE,
                    properties.paths().updateDevice(),
                    NagraException.FailureType.INTEGRATION_DISABLED
            );
        }

        nagraAdmClient.activateDevice(
                source,
                sn,
                entitlement.accountId()
        );

        try {
            nagraRmgClient.createEntitlement(
                    source,
                    sn,
                    entitlement
            );
        } catch (NagraException exception) {
            throw new NagraPartialFailureException(
                    NagraOperation.ADM_ACTIVATE_DEVICE,
                    exception
            );
        }
    }

    public void refreshSmartcard(
            String sn,
            SmartcardType smartcardType,
            SmartcardSource source
    ) {
        if (smartcardType == null) {
            throw new IllegalArgumentException("smartcardType is required");
        }
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }

        if (!properties.enabled()) {
            throw new NagraException(
                    null,
                    "NAGRA integration disabled; refresh was not executed",
                    null,
                    NagraOperation.ADM_REFRESH_ACCOUNT,
                    properties.paths().updateAccount(),
                    NagraException.FailureType.INTEGRATION_DISABLED
            );
        }

        String accountId = sn + "_" + smartcardType.getNagraType();

        nagraAdmClient.refreshAccount(
                source,
                sn,
                accountId
        );
    }

    public void suspendSmartcard(
            String sn,
            SmartcardType smartcardType,
            SmartcardSource source
    ) {
        if (smartcardType == null) {
            throw new IllegalArgumentException("smartcardType is required");
        }
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }

        if (!properties.enabled()) {
            throw new NagraException(
                    null,
                    "NAGRA integration disabled; suspension was not executed",
                    null,
                    NagraOperation.ADM_SUSPEND_ACCOUNT,
                    properties.paths().updateAccount(),
                    NagraException.FailureType.INTEGRATION_DISABLED
            );
        }

        String accountId = sn + "_" + smartcardType.getNagraType();

        nagraAdmClient.suspendAccount(
                source,
                sn,
                accountId
        );

        try {
            nagraRmgClient.deleteEntitlement(
                    source,
                    sn,
                    accountId
            );
        } catch (NagraException exception) {
            throw new NagraPartialFailureException(
                    NagraOperation.ADM_SUSPEND_ACCOUNT,
                    exception
            );
        }
    }

    public void reactivateSmartcard(
            String sn,
            SmartcardSource source,
            RmgEntitlementRequest entitlement
    ) {
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        if (entitlement == null) {
            throw new IllegalArgumentException("entitlement is required");
        }

        if (!properties.enabled()) {
            throw new NagraException(
                    null,
                    "NAGRA integration disabled; reactivation was not executed",
                    null,
                    NagraOperation.ADM_REACTIVATE_ACCOUNT,
                    properties.paths().updateAccount(),
                    NagraException.FailureType.INTEGRATION_DISABLED
            );
        }

        nagraAdmClient.reactivateAccount(
                source,
                sn,
                entitlement.accountId()
        );

        try {
            nagraRmgClient.createEntitlement(
                    source,
                    sn,
                    entitlement
            );
        } catch (NagraException exception) {
            throw new NagraPartialFailureException(
                    NagraOperation.ADM_REACTIVATE_ACCOUNT,
                    exception
            );
        }
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
