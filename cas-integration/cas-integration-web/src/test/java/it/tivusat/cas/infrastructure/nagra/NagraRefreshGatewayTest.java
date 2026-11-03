package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NagraRefreshGatewayTest {

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
    void shouldRefreshAccountWithoutDeviceOrEntitlementCalls() {
        gateway.refreshSmartcard(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        );

        verify(adm).refreshAccount(
                SmartcardSource.PHYSICAL,
                SN,
                ACCOUNT_ID
        );

        verifyNoMoreInteractions(adm);
        verifyNoInteractions(rmg);
    }

    @Test
    void shouldPropagateAccountFailureWithoutFurtherCalls() {
        NagraException failure = new NagraException(
                503,
                "Simulated ADM unavailable"
        );

        doThrow(failure).when(adm).refreshAccount(
                SmartcardSource.PHYSICAL,
                SN,
                ACCOUNT_ID
        );

        NagraException actual = assertThrows(
                NagraException.class,
                () -> gateway.refreshSmartcard(
                        SN,
                        SmartcardType.TIVU_HD,
                        SmartcardSource.PHYSICAL
                )
        );

        assertSame(failure, actual);

        verify(adm).refreshAccount(
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

        NagraProperties.Paths paths =
                mock(NagraProperties.Paths.class);

        when(properties.paths()).thenReturn(paths);
        when(paths.updateAccount())
                .thenReturn("/adm/v1/accounts/{accountId}");

        NagraException actual = assertThrows(
                NagraException.class,
                () -> gateway.refreshSmartcard(
                        SN,
                        SmartcardType.TIVU_HD,
                        SmartcardSource.PHYSICAL
                )
        );

        assertEquals(
                NagraException.FailureType.INTEGRATION_DISABLED,
                actual.getFailureType()
        );
        assertEquals(
                NagraOperation.ADM_REFRESH_ACCOUNT,
                actual.getOperation()
        );

        verifyNoInteractions(adm, rmg);
    }
}