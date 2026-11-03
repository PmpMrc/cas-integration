package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ActivateSmartcardRequest;
import it.tivusat.cas.api.dto.SmartcardResponse;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.exception.SmartcardNotFoundException;
import it.tivusat.cas.infrastructure.nagra.NagraException;
import it.tivusat.cas.infrastructure.nagra.NagraSmartcardStatusSnapshot;
import it.tivusat.cas.infrastructure.nagra.dto.NagraDeviceResponse;
import it.tivusat.cas.infrastructure.nagra.dto.NagraEntitlementResponse;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import org.mockito.ArgumentCaptor;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.domain.SmartcardValidator;
import it.tivusat.cas.domain.UaRangeClassifier;
import it.tivusat.cas.infrastructure.nagra.NagraSmartcardGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartcardOperationsUseCaseTest {

    private static final String SN = "109687603246";
    private static final String UA = "1096876032";

    @Mock
    private NagraSmartcardGateway nagraSmartcardGateway;

    private SmartcardOperationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SmartcardOperationsUseCase(
                new SmartcardValidator(),
                new UaRangeClassifier(),
                nagraSmartcardGateway
        );
    }

    @Test
    void shouldActivateSmartcard() {
        ActivateSmartcardRequest request = new ActivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD,
                null
        );

        SmartcardResponse response = useCase.activate(SN, request);

        assertEquals(SN, response.getSn());
        assertEquals(UA, response.getUa());
        assertEquals(SmartcardStatus.ENABLED, response.getStatus());
        assertEquals(SmartcardSource.PHYSICAL, response.getSource());
        assertEquals(SmartcardType.TIVU_HD, response.getSmartcardType());

        ArgumentCaptor<RmgEntitlementRequest> captor =
                ArgumentCaptor.forClass(RmgEntitlementRequest.class);

        verify(nagraSmartcardGateway).activateSmartcard(
                eq(SN),
                eq(SmartcardSource.PHYSICAL),
                captor.capture()
        );

        RmgEntitlementRequest sent = captor.getValue();

        assertEquals(SN + "_TivuHD", sent.id());
        assertEquals(SN + "_TivuHD", sent.accountId());
        assertEquals("22", sent.productId());
        assertEquals(sent.id(), response.getEntitlementId());
        assertEquals(sent.productId(), response.getProductId());
        assertEquals(sent.expiryDate(), response.getExpiryDate());
    }

    @Test
    void shouldFailPairingActivationWithoutCaSn() {
        ActivateSmartcardRequest request = new ActivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD_PAIRING,
                null
        );

        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                useCase.activate(SN, request);
            }
        });

        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void shouldUsePairingTypeForActivationWithoutEchoingUnverifiedCaSn() {
        ActivateSmartcardRequest request = new ActivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD_PAIRING,
                "1234567890"
        );

        SmartcardResponse response = useCase.activate(SN, request);

        ArgumentCaptor<RmgEntitlementRequest> captor =
                ArgumentCaptor.forClass(RmgEntitlementRequest.class);

        verify(nagraSmartcardGateway).activateSmartcard(
                eq(SN),
                eq(SmartcardSource.PHYSICAL),
                captor.capture()
        );

        assertEquals(
                SN + "_" + SmartcardType.TIVU_HD_PAIRING.getNagraType(),
                captor.getValue().accountId()
        );
        assertEquals(SmartcardStatus.ENABLED, response.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(response.getCaSn());
    }

    @Test
    void shouldRefreshSmartcard() {
        ActivateSmartcardRequest request = new ActivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD,
                null
        );

        SmartcardResponse response = useCase.refresh(SN, request);

        assertEquals(SN, response.getSn());
        assertEquals(UA, response.getUa());
        assertEquals(SmartcardStatus.ENABLED, response.getStatus());
        assertEquals(SmartcardType.TIVU_HD, response.getSmartcardType());

        org.junit.jupiter.api.Assertions.assertNull(response.getEntitlementId());
        org.junit.jupiter.api.Assertions.assertNull(response.getProductId());
        org.junit.jupiter.api.Assertions.assertNull(response.getCaSn());
        org.junit.jupiter.api.Assertions.assertNull(response.getExpiryDate());

        verify(nagraSmartcardGateway).refreshSmartcard(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        );
    }

    @Test
    void shouldSuspendSmartcard() {
        SmartcardResponse response = useCase.suspend(SN, SmartcardSource.PHYSICAL);

        assertEquals(SmartcardStatus.DISABLED, response.getStatus());

        verify(nagraSmartcardGateway).suspendSmartcard(
                eq(SN),
                eq(UA),
                eq(SmartcardSource.PHYSICAL)
        );
    }

    @Test
    void shouldDeleteSmartcard() {
        SmartcardResponse response = useCase.delete(SN, SmartcardSource.PHYSICAL);

        assertEquals(SmartcardStatus.DELETED, response.getStatus());
        assertEquals(false, response.isDeviceCreated());

        verify(nagraSmartcardGateway).deleteSmartcard(
                eq(SN),
                eq(SmartcardSource.PHYSICAL)
        );
    }

    @Test
    void statusWithoutDeviceOrEntitlementIsNotPreloaded() {
        when(nagraSmartcardGateway.fetchSmartcardStatus(SN, SmartcardSource.PHYSICAL))
                .thenReturn(NagraSmartcardStatusSnapshot.deviceNotFound(null));

        assertThrows(SmartcardNotFoundException.class,
                () -> useCase.getStatus(SN, SmartcardSource.PHYSICAL));
    }

    @Test
    void statusWithEntitlementAndNoDeviceIsNotFound() {
        NagraEntitlementResponse entitlement = new NagraEntitlementResponse(
                SN + "_TivuHD", SN, "22", "SUBSCRIBED", "ABSOLUTE",
                "SUBSCRIPTION", null, null);
        when(nagraSmartcardGateway.fetchSmartcardStatus(SN, SmartcardSource.PHYSICAL))
                .thenReturn(NagraSmartcardStatusSnapshot.deviceNotFound(entitlement));

        assertThrows(SmartcardNotFoundException.class,
                () -> useCase.getStatus(SN, SmartcardSource.PHYSICAL));
    }

    @Test
    void statusWithUnexpectedNagraDeviceStateIsNotEnabled() {
        when(nagraSmartcardGateway.fetchSmartcardStatus(SN, SmartcardSource.PHYSICAL))
                .thenReturn(NagraSmartcardStatusSnapshot.of(
                        new NagraDeviceResponse("PENDING", null, UA, SN), null));

        NagraException error = assertThrows(NagraException.class,
                () -> useCase.getStatus(SN, SmartcardSource.PHYSICAL));
        assertEquals(NagraException.FailureType.INVALID_RESPONSE, error.getFailureType());
        assertEquals(NagraOperation.ADM_GET_DEVICE, error.getOperation());
    }

    @Test
    void statusWithDisabledNagraDeviceIsDisabled() {
        when(nagraSmartcardGateway.fetchSmartcardStatus(SN, SmartcardSource.PHYSICAL))
                .thenReturn(NagraSmartcardStatusSnapshot.of(
                        new NagraDeviceResponse("DISABLED", null, UA, SN), null));

        assertEquals(SmartcardStatus.DISABLED,
                useCase.getStatus(SN, SmartcardSource.PHYSICAL).getStatus());
    }

    @Test
    void shouldFailWithInvalidChecksum() {
        ActivateSmartcardRequest request = new ActivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD,
                null
        );

        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                useCase.activate("109687603200", request);
            }
        });

        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void refreshShouldRejectMissingTypeBeforeCallingNagra() {
        ActivateSmartcardRequest request = new ActivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                null,
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.refresh(SN, request)
        );

        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void refreshShouldNotRequireCaSnForPairingType() {
        ActivateSmartcardRequest request = new ActivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD_PAIRING,
                null
        );

        useCase.refresh(SN, request);

        verify(nagraSmartcardGateway).refreshSmartcard(
                SN,
                SmartcardType.TIVU_HD_PAIRING,
                SmartcardSource.PHYSICAL
        );
    }

    @Test
    void refreshShouldPropagateNagraFailure() {
        ActivateSmartcardRequest request = new ActivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD,
                null
        );

        NagraException failure = new NagraException(
                503,
                "Simulated ADM unavailable"
        );

        org.mockito.Mockito.doThrow(failure)
                .when(nagraSmartcardGateway)
                .refreshSmartcard(
                        SN,
                        SmartcardType.TIVU_HD,
                        SmartcardSource.PHYSICAL
                );

        NagraException actual = assertThrows(
                NagraException.class,
                () -> useCase.refresh(SN, request)
        );

        org.junit.jupiter.api.Assertions.assertSame(failure, actual);
    }
}