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

class NagraActivationGatewayTest {

    private static final String SN = "109687603246";
    private static final String DEVICE_ID = SN + "_TivuHD";
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
                Instant.parse("2026-10-29T17:03:16Z")
        );
    }

    @Test
    void shouldUpdateDeviceBeforeCreatingEntitlement() {
        gateway.activateSmartcard(SN, SOURCE, entitlement);

        InOrder order = inOrder(adm, rmg);

        order.verify(adm).activateDevice(SOURCE, SN, DEVICE_ID);
        order.verify(rmg).createEntitlement(SOURCE, SN, entitlement);

        verifyNoMoreInteractions(adm, rmg);
    }

    @Test
    void shouldNotCreateEntitlementWhenDeviceUpdateFails() {
        NagraException failure = new NagraException(
                503,
                "Simulated ADM unavailable"
        );

        doThrow(failure).when(adm)
                .activateDevice(SOURCE, SN, DEVICE_ID);

        NagraException actual = assertThrows(
                NagraException.class,
                () -> gateway.activateSmartcard(SN, SOURCE, entitlement)
        );

        assertSame(failure, actual);
        verifyNoInteractions(rmg);
    }

    @Test
    void shouldReportPartialFailureWhenEntitlementFails() {
        NagraException failure = new NagraException(
                503,
                "Simulated RMG unavailable",
                null,
                NagraOperation.RMG_CREATE_ENTITLEMENT,
                "/rmg/v1/operator/entitlements",
                NagraException.FailureType.HTTP_ERROR,
                "{\"productId\":\"22\"}"
        );

        doThrow(failure).when(rmg)
                .createEntitlement(SOURCE, SN, entitlement);

        NagraPartialFailureException actual = assertThrows(
                NagraPartialFailureException.class,
                () -> gateway.activateSmartcard(SN, SOURCE, entitlement)
        );

        assertSame(failure, actual.getCause());
        assertEquals(
                NagraOperation.ADM_ACTIVATE_DEVICE,
                actual.getCompletedOperation()
        );
        assertEquals(
                NagraOperation.RMG_CREATE_ENTITLEMENT,
                actual.getOperation()
        );
        assertEquals(Integer.valueOf(503), actual.getHttpStatus());
        assertEquals(failure.getRequestPayload(), actual.getRequestPayload());

        InOrder order = inOrder(adm, rmg);
        order.verify(adm).activateDevice(SOURCE, SN, DEVICE_ID);
        order.verify(rmg).createEntitlement(SOURCE, SN, entitlement);

        verifyNoMoreInteractions(adm, rmg);
    }

    @Test
    void shouldFailWithoutCallsWhenIntegrationIsDisabled() {
        when(properties.enabled()).thenReturn(false);

        NagraProperties.Paths paths = mock(NagraProperties.Paths.class);
        when(properties.paths()).thenReturn(paths);
        when(paths.updateDevice())
                .thenReturn("/adm/v1/devices/{deviceId}");

        NagraException actual = assertThrows(
                NagraException.class,
                () -> gateway.activateSmartcard(SN, SOURCE, entitlement)
        );

        assertEquals(
                NagraException.FailureType.INTEGRATION_DISABLED,
                actual.getFailureType()
        );
        verifyNoInteractions(adm, rmg);
    }
}