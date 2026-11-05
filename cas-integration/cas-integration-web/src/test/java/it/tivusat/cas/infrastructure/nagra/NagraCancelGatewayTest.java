package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NagraCancelGatewayTest {

    private static final String SN = "109687603246";
    private static final String ACCOUNT_ID = SN + "_TivuHD";

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
    void shouldCancelAccountWithoutDeviceDeletionOrRmgCalls() {
        cancel();

        verify(adm).cancelAccount(
                SmartcardSource.PHYSICAL,
                SN,
                ACCOUNT_ID
        );

        verifyNoMoreInteractions(adm);
        verifyNoInteractions(rmg);
    }

    @Test
    void shouldPropagateCancellationFailureWithoutFurtherCalls() {
        NagraException failure =
                new NagraException(503, "Simulated ADM unavailable");

        doThrow(failure).when(adm).cancelAccount(
                SmartcardSource.PHYSICAL,
                SN,
                ACCOUNT_ID
        );

        NagraException actual = assertThrows(
                NagraException.class,
                this::cancel
        );

        assertSame(failure, actual);

        verify(adm).cancelAccount(
                SmartcardSource.PHYSICAL,
                SN,
                ACCOUNT_ID
        );

        verifyNoMoreInteractions(adm);
        verifyNoInteractions(rmg);
    }

    @Test
    void shouldFailWithoutCallsWhenIntegrationIsDisabled() {
        when(properties.enabled()).thenReturn(false);

        NagraProperties.Paths paths = mock(NagraProperties.Paths.class);
        when(properties.paths()).thenReturn(paths);
        when(paths.cancelAccount())
                .thenReturn("/adm/v1/accounts/{accountId}");

        NagraException actual = assertThrows(
                NagraException.class,
                this::cancel
        );

        assertEquals(
                NagraException.FailureType.INTEGRATION_DISABLED,
                actual.getFailureType()
        );
        assertEquals(
                NagraOperation.ADM_CANCEL_ACCOUNT,
                actual.getOperation()
        );

        verifyNoInteractions(adm, rmg);
    }

    private void cancel() {
        gateway.deleteSmartcard(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        );
    }
}