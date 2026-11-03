package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NagraSuspendGatewayTest {

    private static final String SN = "109687603246";
    private static final String ACCOUNT_ID = SN + "_TivuHD";
    private static final SmartcardSource SOURCE = SmartcardSource.PHYSICAL;

    private NagraProperties properties;
    private NagraAdmClient adm;
    private NagraRmgClient rmg;
    private NagraSmartcardGateway gateway;

    @BeforeEach
    void setUp() {
        properties = mock(NagraProperties.class);
        adm = mock(NagraAdmClient.class);
        rmg = mock(NagraRmgClient.class);

        when(properties.enabled()).thenReturn(true);

        gateway = new NagraSmartcardGateway(properties, adm, rmg);
    }

    @Test
    void shouldSuspendAccountBeforeDeletingEntitlement() {
        suspend();

        InOrder order = inOrder(adm, rmg);

        order.verify(adm).suspendAccount(SOURCE, SN, ACCOUNT_ID);
        order.verify(rmg).deleteEntitlement(SOURCE, SN, ACCOUNT_ID);

        verifyNoMoreInteractions(adm, rmg);
    }

    @Test
    void shouldStopWhenAccountSuspensionFails() {
        NagraException failure =
                new NagraException(503, "Simulated ADM unavailable");

        doThrow(failure).when(adm)
                .suspendAccount(SOURCE, SN, ACCOUNT_ID);

        NagraException actual = assertThrows(
                NagraException.class,
                this::suspend
        );

        assertSame(failure, actual);

        verify(adm).suspendAccount(SOURCE, SN, ACCOUNT_ID);
        verifyNoMoreInteractions(adm);
        verifyNoInteractions(rmg);
    }

    @Test
    void shouldReportPartialFailureWhenEntitlementDeletionFails() {
        NagraException failure = new NagraException(
                503,
                "Simulated RMG unavailable",
                null,
                NagraOperation.RMG_DELETE_ENTITLEMENT,
                "/rmg/v1/operator/entitlements/" + ACCOUNT_ID,
                NagraException.FailureType.HTTP_ERROR
        );

        doThrow(failure).when(rmg)
                .deleteEntitlement(SOURCE, SN, ACCOUNT_ID);

        NagraPartialFailureException actual = assertThrows(
                NagraPartialFailureException.class,
                this::suspend
        );

        assertSame(failure, actual.getCause());
        assertEquals(
                NagraOperation.ADM_SUSPEND_ACCOUNT,
                actual.getCompletedOperation()
        );
        assertEquals(
                NagraOperation.RMG_DELETE_ENTITLEMENT,
                actual.getOperation()
        );
        assertEquals(Integer.valueOf(503), actual.getHttpStatus());

        InOrder order = inOrder(adm, rmg);

        order.verify(adm).suspendAccount(SOURCE, SN, ACCOUNT_ID);
        order.verify(rmg).deleteEntitlement(SOURCE, SN, ACCOUNT_ID);

        verifyNoMoreInteractions(adm, rmg);
    }

    @Test
    void shouldFailWithoutCallsWhenIntegrationIsDisabled() {
        when(properties.enabled()).thenReturn(false);

        NagraProperties.Paths paths =
                mock(NagraProperties.Paths.class);

        when(properties.paths()).thenReturn(paths);
        when(paths.updateAccount())
                .thenReturn("/adm/v1/accounts/{accountId}");

        NagraException actual = assertThrows(
                NagraException.class,
                this::suspend
        );

        assertEquals(
                NagraException.FailureType.INTEGRATION_DISABLED,
                actual.getFailureType()
        );
        assertEquals(
                NagraOperation.ADM_SUSPEND_ACCOUNT,
                actual.getOperation()
        );

        verifyNoInteractions(adm, rmg);
    }

    private void suspend() {
        gateway.suspendSmartcard(
                SN,
                SmartcardType.TIVU_HD,
                SOURCE
        );
    }
}