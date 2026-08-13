package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.dto.AdmAccountRequest;
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
}