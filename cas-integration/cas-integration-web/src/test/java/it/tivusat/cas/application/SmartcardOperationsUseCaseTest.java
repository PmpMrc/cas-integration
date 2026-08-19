package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ActivateSmartcardRequest;
import it.tivusat.cas.api.dto.SmartcardResponse;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
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

        verify(nagraSmartcardGateway).activateSmartcard(
                eq(SN),
                eq(UA),
                eq(SmartcardSource.PHYSICAL),
                eq((String) null)
        );
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
    void shouldActivatePairingWithCaSn() {
        ActivateSmartcardRequest request = new ActivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD_PAIRING,
                "1234567890"
        );

        SmartcardResponse response = useCase.activate(SN, request);

        assertEquals(SmartcardStatus.ENABLED, response.getStatus());
        assertEquals("1234567890", response.getCaSn());

        verify(nagraSmartcardGateway).activateSmartcard(
                eq(SN),
                eq(UA),
                eq(SmartcardSource.PHYSICAL),
                eq("1234567890")
        );
    }

    @Test
    void shouldRefreshSmartcard() {
        ActivateSmartcardRequest request = new ActivateSmartcardRequest(
                SmartcardSource.PHYSICAL,
                SmartcardType.TIVU_HD,
                null
        );

        SmartcardResponse response = useCase.refresh(SN, request);

        assertEquals(SmartcardStatus.ENABLED, response.getStatus());

        verify(nagraSmartcardGateway).refreshSmartcard(
                eq(SN),
                eq(UA),
                eq(SmartcardSource.PHYSICAL),
                eq((String) null)
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

        verify(nagraSmartcardGateway).deleteSmartcard(
                eq(SN),
                eq(SmartcardSource.PHYSICAL)
        );
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
}