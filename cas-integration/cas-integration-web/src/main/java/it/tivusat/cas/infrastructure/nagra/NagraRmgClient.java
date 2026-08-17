package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.infrastructure.nagra.dto.NagraEntitlementResponse;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class NagraRmgClient {

    private final NagraProperties properties;
    private final NagraRestExecutor executor;

    public NagraRmgClient(
            NagraProperties properties,
            NagraRestExecutor executor
    ) {
        this.properties = properties;
        this.executor = executor;
    }

    public void createEntitlement(
            SmartcardSource source,
            RmgEntitlementRequest request
    ) {
        executor.execute(
                NagraOperation.RMG_CREATE_ENTITLEMENT,
                request.accountId(),
                HttpMethod.POST,
                properties.paths().createEntitlement(),
                source,
                "W",
                request
        );
    }

    public NagraEntitlementResponse getEntitlementsByAccountId(
            SmartcardSource source,
            String sn
    ) {
        String filter = "{\"accountId\":\"" + sn + "\"}";

        return executor.execute(
                NagraOperation.RMG_GET_ENTITLEMENTS,
                sn,
                HttpMethod.GET,
                properties.paths().getEntitlements() + "?filter={filter}",
                source,
                "W",
                null,
                NagraEntitlementResponse.class,
                filter
        );
    }
}