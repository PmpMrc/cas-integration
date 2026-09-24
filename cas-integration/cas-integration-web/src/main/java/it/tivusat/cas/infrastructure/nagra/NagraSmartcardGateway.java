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
            List<String> mismatches = new ArrayList<>();
            if (!entitlementRequest.id().equals(existing.id())) {
                mismatches.add("_id expected " + entitlementRequest.id() + ", got " + existing.id());
            }
            if (!sn.equals(existing.accountId())) {
                mismatches.add("accountId expected " + sn + ", got " + existing.accountId());
            }
            if (!productId.equals(existing.productId())) {
                mismatches.add("productId expected " + productId + ", got " + existing.productId());
            }
            if (!"SUBSCRIBED".equalsIgnoreCase(existing.status())) {
                mismatches.add("status expected SUBSCRIBED, got " + existing.status());
            }
            if (!"ABSOLUTE".equalsIgnoreCase(existing.validityType())) {
                mismatches.add("validityType expected ABSOLUTE, got " + existing.validityType());
            }
            if (!"SUBSCRIPTION".equalsIgnoreCase(existing.productType())) {
                mismatches.add("productType expected SUBSCRIPTION, got " + existing.productType());
            }
            if (existing.expiryDate() == null || !existing.expiryDate().isAfter(Instant.now())) {
                mismatches.add("expiryDate must be in the future, got " + existing.expiryDate());
            }
            if (!mismatches.isEmpty()) {
                throw new IllegalStateException(
                        "Cannot complete preload for smartcard " + sn
                                + ": existing RMG entitlement differs: " + String.join("; ", mismatches)
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

        if (entitlement == null) {
            throw EntitlementValidationException.notFound(sn);
        }
        if (entitlement.accountId() == null || entitlement.accountId().trim().isEmpty()) {
            throw EntitlementValidationException.missingAccountId(
                    sn, entitlement.id(), entitlement.status()
            );
        }
        if (!sn.equals(entitlement.accountId())) {
            throw EntitlementValidationException.accountMismatch(
                    sn, entitlement.accountId(), entitlement.status(), entitlement.id()
            );
        }
        if (entitlement.status() == null || entitlement.status().trim().isEmpty()) {
            throw EntitlementValidationException.missingStatus(sn, entitlement.id());
        }
        if (!"SUBSCRIBED".equalsIgnoreCase(entitlement.status())) {
            throw EntitlementValidationException.statusMismatch(
                    sn, entitlement.status(), entitlement.id()
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
