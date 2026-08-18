package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.dto.AdmAccountRequest;
import it.tivusat.cas.infrastructure.nagra.dto.AdmDeviceRequest;
import it.tivusat.cas.infrastructure.nagra.dto.NagraDeviceResponse;
import it.tivusat.cas.infrastructure.nagra.dto.NagraEntitlementResponse;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

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
            SmartcardType smartcardType,
            SmartcardSource source,
            String productId,
            Instant validityFrom,
            Instant expiryDate
    ) {
        if (!properties.enabled()) {
            log.warn("NAGRA integration disabled. Skipping preload for smartcard {}", sn);
            return;
        }

        nagraAdmClient.createAccount(source, AdmAccountRequest.active(sn));

        RmgEntitlementRequest entitlementRequest =
                RmgEntitlementRequest.subscription(
                        smartcardType.getNagraType(),
                        sn,
                        productId,
                        validityFrom,
                        expiryDate
                );

        nagraRmgClient.createEntitlement(source, entitlementRequest);
    }

    public void activateSmartcard(String sn, String ua, SmartcardSource source, String caSn) {
        if (!properties.enabled()) {
            log.warn("NAGRA integration disabled. Skipping activation for smartcard {}", sn);
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