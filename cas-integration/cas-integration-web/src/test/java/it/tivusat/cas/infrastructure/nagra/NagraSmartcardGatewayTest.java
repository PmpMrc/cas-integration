package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.dto.AdmBundledAccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class NagraSmartcardGatewayTest {

    private static final String SN = "109687603246";
    private static final String UA = "1096876032";
    private static final SmartcardSource SOURCE =
            SmartcardSource.PHYSICAL;

    @Mock
    private NagraAdmClient adm;

    @Mock
    private NagraRmgClient rmg;

    private NagraProperties properties;
    private NagraSmartcardGateway gateway;

    @BeforeEach
    void setUp() {
        properties = new NagraProperties();
        properties.setEnabled(true);
        properties.paths().setCreateBundledAccount(
                "/adm/v1/bundled/accounts"
        );

        gateway = new NagraSmartcardGateway(properties, adm, rmg);
    }

    private void preload() {
        gateway.preloadSmartcard(
                SN,
                UA,
                SmartcardType.TIVU_HD,
                SOURCE
        );
    }

    @Test
    void shouldCreateBundledAccountWithoutCallingRmg() {
        preload();

        ArgumentCaptor<AdmBundledAccountRequest> captor =
                ArgumentCaptor.forClass(AdmBundledAccountRequest.class);

        verify(adm).createBundledAccount(
                eq(SOURCE),
                eq(SN),
                captor.capture()
        );

        AdmBundledAccountRequest request = captor.getValue();

        assertEquals(SN + "_TivuHD", request.id());
        assertEquals("ACTIVE", request.status());
        assertEquals(1, request.getDevices().size());

        assertEquals(
                SN + "_TivuHD",
                request.getDevices().get(0).id()
        );
        assertEquals(
                "DISABLED",
                request.getDevices().get(0).status()
        );
        assertEquals(
                UA,
                request.getDevices().get(0).smartCardId()
        );
        assertEquals(
                SN + "_TivuHD",
                request.getDevices().get(0).accountUid()
        );

        verifyNoMoreInteractions(adm);
        verifyNoInteractions(rmg);
    }

    @Test
    void shouldPropagateBundledFailureWithoutCallingRmg() {
        assertPreloadFailureIsPropagated(
                new NagraException(503, "Service unavailable")
        );
    }

    @Test
    void shouldNotTreatDuplicateAccountAsSuccessfulPreload() {
        assertPreloadFailureIsPropagated(
                new NagraException(409, "Account already exists")
        );
    }

    @Test
    void shouldNotReportSuccessWhenIntegrationIsDisabled() {
        properties.setEnabled(false);

        NagraException failure =
                assertThrows(NagraException.class, this::preload);

        assertEquals(
                NagraException.FailureType.INTEGRATION_DISABLED,
                failure.getFailureType()
        );

        verifyNoInteractions(adm, rmg);
    }

    private void assertPreloadFailureIsPropagated(
            NagraException expected
    ) {
        doThrow(expected)
                .when(adm)
                .createBundledAccount(
                        eq(SOURCE),
                        eq(SN),
                        any(AdmBundledAccountRequest.class)
                );

        NagraException actual =
                assertThrows(NagraException.class, this::preload);

        assertSame(expected, actual);

        verify(adm).createBundledAccount(
                eq(SOURCE),
                eq(SN),
                any(AdmBundledAccountRequest.class)
        );

        verifyNoMoreInteractions(adm);
        verifyNoInteractions(rmg);
    }
}