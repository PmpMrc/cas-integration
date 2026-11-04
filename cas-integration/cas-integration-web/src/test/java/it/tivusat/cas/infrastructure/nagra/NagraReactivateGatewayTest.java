package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NagraReactivateGatewayTest {

    private static final String SN = "109687603246";
    private static final String ACCOUNT_ID = SN + "_TivuHD";
    private static final SmartcardSource SOURCE = SmartcardSource.PHYSICAL;

    private NagraProperties properties;
    private NagraAdmClient adm;
    private NagraRmgClient rmg;
    private NagraSmartcardGateway gateway;
    private RmgEntitlementRequest entitlement;

    @BeforeEach
    void setUp() {
        properties = mock(NagraProperties.class);
        adm = mock(NagraAdmClient.class);
        rmg = mock(NagraRmgClient.class);

        when(properties.enabled()).thenReturn(true);

        gateway = new NagraSmartcardGateway(properties, adm, rmg);

        entitlement = SubscriptionEntitlementFactory.create(
                SN,
                SmartcardType.TIVU_HD,
                Instant.parse("2026-11-04T10:00:00Z")
        );
    }

    @Test
    void shouldReactivateAccountBeforeCreatingEntitlement() {
        reactivate();

        InOrder order = inOrder(adm, rmg);
        order.verify(adm).reactivateAccount(SOURCE, SN, ACCOUNT_ID);
        order.verify(rmg).createEntitlement(SOURCE, SN, entitlement);

        verifyNoMoreInteractions(adm, rmg);
    }

    @Test
    void shouldStopWhenAccountReactivationFails() {
        NagraException failure =
                new NagraException(503, "Simulated ADM unavailable");

        doThrow(failure).when(adm)
                .reactivateAccount(SOURCE, SN, ACCOUNT_ID);

        NagraException actual = assertThrows(
                NagraException.class,
                this::reactivate
        );

        assertSame(failure, actual);
        verify(adm).reactivateAccount(SOURCE, SN, ACCOUNT_ID);
        verifyNoMoreInteractions(adm);
        verifyNoInteractions(rmg);
    }

    @Test
    void shouldReportPartialFailureWhenEntitlementCreationFails() {
        NagraException failure = new NagraException(
                503,
                "Simulated RMG unavailable",
                null,
                NagraOperation.RMG_CREATE_ENTITLEMENT,
                "/rmg/v1/operator/entitlements",
                NagraException.FailureType.HTTP_ERROR
        );

        doThrow(failure).when(rmg)
                .createEntitlement(SOURCE, SN, entitlement);

        NagraPartialFailureException actual = assertThrows(
                NagraPartialFailureException.class,
                this::reactivate
        );

        assertSame(failure, actual.getCause());
        assertEquals(
                NagraOperation.ADM_REACTIVATE_ACCOUNT,
                actual.getCompletedOperation()
        );
        assertEquals(
                NagraOperation.RMG_CREATE_ENTITLEMENT,
                actual.getOperation()
        );
        assertEquals(Integer.valueOf(503), actual.getHttpStatus());

        InOrder order = inOrder(adm, rmg);
        order.verify(adm).reactivateAccount(SOURCE, SN, ACCOUNT_ID);
        order.verify(rmg).createEntitlement(SOURCE, SN, entitlement);

        verifyNoMoreInteractions(adm, rmg);
    }

    @Test
    void shouldFailWithoutCallsWhenIntegrationIsDisabled() {
        when(properties.enabled()).thenReturn(false);

        NagraProperties.Paths paths = mock(NagraProperties.Paths.class);
        when(properties.paths()).thenReturn(paths);
        when(paths.updateAccount())
                .thenReturn("/adm/v1/accounts/{accountId}");

        NagraException actual = assertThrows(
                NagraException.class,
                this::reactivate
        );

        assertEquals(
                NagraException.FailureType.INTEGRATION_DISABLED,
                actual.getFailureType()
        );
        verifyNoInteractions(adm, rmg);
    }

    private void reactivate() {
        gateway.reactivateSmartcard(SN, SOURCE, entitlement);
    }
}