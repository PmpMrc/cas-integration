package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.dto.AdmAccountRequest;
import it.tivusat.cas.infrastructure.nagra.dto.AdmDeviceRequest;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NagraSmartcardGateway {

    private static final Logger log = LoggerFactory.getLogger(NagraSmartcardGateway.class);

    private final NagraProperties properties;
    private final NagraAdmClient admClient;
    private final NagraRmgClient rmgClient;

    public NagraSmartcardGateway(
            NagraProperties properties,
            NagraAdmClient admClient,
            NagraRmgClient rmgClient
    ) {
        this.properties = properties;
        this.admClient = admClient;
        this.rmgClient = rmgClient;
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
            log.info(
                    "NAGRA integration disabled. Skipping preload calls for smartcard SN {}",
                    sn
            );
            return;
        }

        admClient.createAccount(
                source,
                AdmAccountRequest.active(sn)
        );

        rmgClient.createEntitlement(
                source,
                RmgEntitlementRequest.subscription(
                        smartcardType.getNagraType(),
                        sn,
                        productId,
                        validityFrom,
                        expiryDate
                )
        );
    }

    public void activateSmartcard(
            String sn,
            String ua,
            SmartcardSource source,
            String caSn
    ) {
        if (!properties.enabled()) {
            log.info("NAGRA integration disabled. Skipping activate call for smartcard SN {}", sn);
            return;
        }

        admClient.createDevice(
                source,
                AdmDeviceRequest.activate(sn, ua, caSn)
        );
    }

    public void refreshSmartcard(
            String sn,
            String ua,
            SmartcardSource source,
            String caSn
    ) {
        if (!properties.enabled()) {
            log.info("NAGRA integration disabled. Skipping refresh call for smartcard SN {}", sn);
            return;
        }

        admClient.updateDeviceForRefresh(
                source,
                sn,
                AdmDeviceRequest.refresh(sn, ua, caSn)
        );
    }

    public void suspendSmartcard(
            String sn,
            SmartcardSource source
    ) {
        if (!properties.enabled()) {
            log.info("NAGRA integration disabled. Skipping suspend call for smartcard SN {}", sn);
            return;
        }

        admClient.suspendDevice(source, sn);
    }

    public void deleteSmartcard(
            String sn,
            SmartcardSource source
    ) {
        if (!properties.enabled()) {
            log.info("NAGRA integration disabled. Skipping delete call for smartcard SN {}", sn);
            return;
        }

        admClient.deleteDevice(source, sn);
    }
}