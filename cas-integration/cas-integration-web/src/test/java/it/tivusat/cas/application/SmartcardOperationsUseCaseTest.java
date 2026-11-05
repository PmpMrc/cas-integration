package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ActivateSmartcardRequest;
import it.tivusat.cas.api.dto.ReactivateSmartcardRequest;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.tivusat.cas.infrastructure.nagra.dto.NagraBundledAccountResponse;

import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
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
        assertNull(response.getCaSn());
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

        assertNull(response.getEntitlementId());
        assertNull(response.getProductId());
        assertNull(response.getCaSn());
        assertNull(response.getExpiryDate());

        verify(nagraSmartcardGateway).refreshSmartcard(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        );
    }

    @Test
    void shouldSuspendSmartcard() {
        SmartcardResponse response = useCase.suspend(
                SN,
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD
        );

        assertEquals(SmartcardStatus.DISABLED, response.getStatus());
        assertEquals(SmartcardType.TIVU_HD, response.getSmartcardType());

        assertNull(response.getEntitlementId());
        assertNull(response.getProductId());
        assertNull(response.getExpiryDate());

        verify(nagraSmartcardGateway).suspendSmartcard(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        );
    }

    @Test
    void suspendShouldRejectMissingTypeBeforeCallingNagra() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.suspend(
                        SN,
                        SmartcardSource.PHYSICAL,
                        null
                )
        );

        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void shouldDeleteSmartcard() {
        SmartcardResponse response = useCase.delete(
                SN,
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD
        );

        assertEquals(SmartcardStatus.DELETED, response.getStatus());
        assertEquals(SmartcardType.TIVU_HD, response.getSmartcardType());
        assertEquals(true, response.isAccountCreated());
        assertEquals(true, response.isDeviceCreated());

        assertNull(response.getEntitlementId());
        assertNull(response.getProductId());
        assertNull(response.getExpiryDate());

        verify(nagraSmartcardGateway).deleteSmartcard(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL,
                false
        );
    }

    @Test
    void statusShouldReturnBundledAccountDatesAndEntitlementData()
            throws Exception {

        ObjectMapper mapper =
                new ObjectMapper().registerModule(new JavaTimeModule());

        NagraBundledAccountResponse account = mapper.readValue(
                "{\"_id\":\"" + SN + "_TivuHD\","
                        + "\"status\":\"ACTIVE\","
                        + "\"created\":\"2026-10-01T08:00:00Z\","
                        + "\"modified\":\"2026-11-05T09:00:00Z\","
                        + "\"devices\":[{"
                        + "\"deviceId\":\"" + SN + "_TivuHD\","
                        + "\"smartCardId\":\"" + UA + "\","
                        + "\"caSN\":\"1234567890\","
                        + "\"device_status\":\"ENABLED\""
                        + "}]}",
                NagraBundledAccountResponse.class
        );

        NagraEntitlementResponse entitlement = new NagraEntitlementResponse(
                SN + "_TivuHD",
                SN + "_TivuHD",
                "22",
                "SUBSCRIBED",
                "ABSOLUTE",
                "SUBSCRIPTION",
                Instant.parse("2026-11-05T09:00:00Z"),
                Instant.parse("2030-11-05T09:00:00Z")
        );

        when(nagraSmartcardGateway.fetchSmartcardStatus(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        )).thenReturn(NagraSmartcardStatusSnapshot.bundled(
                account,
                account.getDevices().get(0),
                entitlement,
                SmartcardStatus.ENABLED
        ));

        SmartcardResponse response = useCase.getStatus(
                SN,
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD
        );

        assertEquals(SmartcardStatus.ENABLED, response.getStatus());
        assertEquals(SmartcardType.TIVU_HD, response.getSmartcardType());
        assertEquals(SN + "_TivuHD", response.getEntitlementId());
        assertEquals("22", response.getProductId());
        assertEquals("1234567890", response.getCaSn());
        assertEquals(entitlement.expiryDate(), response.getExpiryDate());

        assertEquals(
                Instant.parse("2026-10-01T08:00:00Z"),
                response.getCreatedAt()
        );
        assertEquals(
                Instant.parse("2026-11-05T09:00:00Z"),
                response.getUpdatedAt()
        );
    }

    @Test
    void statusShouldAllowPreloadedCardWithoutEntitlement() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        NagraBundledAccountResponse account = mapper.readValue(
                "{\"_id\":\"" + SN + "_TivuHD\","
                        + "\"status\":\"ACTIVE\","
                        + "\"devices\":[{"
                        + "\"deviceId\":\"" + SN + "_TivuHD\","
                        + "\"device_status\":\"DISABLED\""
                        + "}]}",
                NagraBundledAccountResponse.class
        );

        when(nagraSmartcardGateway.fetchSmartcardStatus(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        )).thenReturn(NagraSmartcardStatusSnapshot.bundled(
                account,
                account.getDevices().get(0),
                null,
                SmartcardStatus.PRELOADED
        ));

        SmartcardResponse response = useCase.getStatus(
                SN,
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD
        );

        assertEquals(SmartcardStatus.PRELOADED, response.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(response.getEntitlementId());
        org.junit.jupiter.api.Assertions.assertNull(response.getProductId());
        org.junit.jupiter.api.Assertions.assertNull(response.getExpiryDate());
        org.junit.jupiter.api.Assertions.assertNull(response.getCreatedAt());
        org.junit.jupiter.api.Assertions.assertNull(response.getUpdatedAt());
    }

    @Test
    void statusShouldRejectMissingTypeBeforeCallingNagra() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.getStatus(
                        SN,
                        SmartcardSource.PHYSICAL,
                        null
                )
        );

        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void statusShouldPropagateGatewayFailure() {
        NagraException failure =
                new NagraException(503, "Simulated ADM unavailable");

        when(nagraSmartcardGateway.fetchSmartcardStatus(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        )).thenThrow(failure);

        NagraException actual = assertThrows(
                NagraException.class,
                () -> useCase.getStatus(
                        SN,
                        SmartcardSource.PHYSICAL,
                        SmartcardType.TIVU_HD
                )
        );

        org.junit.jupiter.api.Assertions.assertSame(failure, actual);
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

    @Test
    void shouldReactivateWithNewEntitlement() {
        ReactivateSmartcardRequest request = new ReactivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD
        );

        SmartcardResponse response = useCase.reactivate(SN, request);

        ArgumentCaptor<RmgEntitlementRequest> captor =
                ArgumentCaptor.forClass(RmgEntitlementRequest.class);

        verify(nagraSmartcardGateway).reactivateSmartcard(
                eq(SN),
                eq(SmartcardSource.PHYSICAL),
                captor.capture()
        );

        RmgEntitlementRequest sent = captor.getValue();

        assertEquals(SN + "_TivuHD", sent.id());
        assertEquals(SN + "_TivuHD", sent.accountId());
        assertEquals("22", sent.productId());
        assertEquals(
                sent.validFrom().atZone(java.time.ZoneOffset.UTC)
                        .plusYears(4).toInstant(),
                sent.expiryDate()
        );

        assertEquals(SmartcardStatus.ENABLED, response.getStatus());
        assertEquals(sent.id(), response.getEntitlementId());
        assertEquals(sent.productId(), response.getProductId());
        assertEquals(sent.expiryDate(), response.getExpiryDate());
        assertNull(response.getCaSn());
    }

    @Test
    void reactivationShouldRejectMissingType() {
        ReactivateSmartcardRequest request = new ReactivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.reactivate(SN, request)
        );

        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void deleteShouldRejectMissingTypeBeforeCallingNagra() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.delete(
                        SN,
                        SmartcardSource.PHYSICAL,
                        null
                )
        );

        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void deleteShouldRejectPurgeForPhysicalSource() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.delete(
                        SN,
                        SmartcardSource.PHYSICAL,
                        SmartcardType.TIVU_HD,
                        true
                )
        );

        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void deleteWithPurgeShouldReturnAccountAndDeviceAbsent() {
        SmartcardResponse response = useCase.delete(
                SN,
                SmartcardSource.VIRTUAL,
                SmartcardType.TIVU_HD,
                true
        );

        assertEquals(SmartcardStatus.DELETED, response.getStatus());
        assertEquals(false, response.isAccountCreated());
        assertEquals(false, response.isDeviceCreated());

        verify(nagraSmartcardGateway).deleteSmartcard(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.VIRTUAL,
                true
        );
    }
}