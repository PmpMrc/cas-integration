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

        try {
            nagraAdmClient.createAccount(source, AdmAccountRequest.active(sn));
        } catch (NagraException exception) {
            if (!exception.isAlreadyExists()) {
                throw exception;
            }
            log.info("ADM account already exists for smartcard {}; continuing preload", sn);
        }

        RmgEntitlementRequest entitlementRequest =
                RmgEntitlementRequest.subscription(
                        sn + "_" + smartcardType.getNagraType(),
                        sn,
                        productId,
                        validityFrom,
                        expiryDate
                );

        try {
            nagraRmgClient.createEntitlement(source, entitlementRequest);
        } catch (NagraException exception) {
            if (!exception.isAlreadyExists()) {
                throw exception;
            }

            NagraEntitlementResponse existing =
                    nagraRmgClient.getEntitlementsByAccountId(source, sn);
            if (existing == null) {
                throw exception;
            }
            if (!entitlementRequest.id().equals(existing.id())
                    || !sn.equals(existing.accountId())
                    || !productId.equals(existing.productId())
                    || !"SUBSCRIBED".equalsIgnoreCase(existing.status())
                    || !"ABSOLUTE".equalsIgnoreCase(existing.validityType())
                    || !"SUBSCRIPTION".equalsIgnoreCase(existing.productType())
                    || existing.expiryDate() == null
                    || !existing.expiryDate().isAfter(Instant.now())) {
                throw new IllegalStateException(
                        "Existing NAGRA entitlement does not match preload for smartcard " + sn
                );
            }
            log.info("RMG entitlement already exists for smartcard {}; preload complete", sn);
        }
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

        NagraEntitlementResponse entitlement =
                nagraRmgClient.getEntitlementsByAccountId(source, sn);

        if (entitlement == null
                || !sn.equals(entitlement.accountId())
                || !"SUBSCRIBED".equalsIgnoreCase(entitlement.status())) {
            throw new IllegalStateException(
                    "Cannot create device: no active entitlement found for smartcard " + sn
            );
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
