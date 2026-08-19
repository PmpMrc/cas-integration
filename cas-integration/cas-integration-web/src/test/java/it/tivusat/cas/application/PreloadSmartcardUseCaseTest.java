package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.api.dto.SmartcardResponse;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.domain.SmartcardValidator;
import it.tivusat.cas.domain.UaRangeClassifier;
import it.tivusat.cas.domain.exception.UnsupportedSmartcardRangeException;
import it.tivusat.cas.infrastructure.nagra.NagraSmartcardGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PreloadSmartcardUseCaseTest {

    private static final String SN = "109687603246";
    private static final String UA = "1096876032";
    private static final String PRODUCT_ID = "PRODUCT_TEST";

    @Mock
    private NagraSmartcardGateway nagraSmartcardGateway;

    private PreloadSmartcardUseCase useCase;

    private SmartcardValidator smartcardValidator;

    @BeforeEach
    void setUp() {
        smartcardValidator = new SmartcardValidator();

        useCase = new PreloadSmartcardUseCase(
                smartcardValidator,
                nagraSmartcardGateway,
                new UaRangeClassifier()
        );
    }

    @Test
    void shouldPreloadSmartcardAndReturnPreloadedResponse() {
        PreloadSmartcardRequest request = new PreloadSmartcardRequest(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL,
                PRODUCT_ID
        );

        SmartcardResponse response = useCase.preload(request);

        assertEquals(SN, response.getSn());
        assertEquals(UA, response.getUa());
        assertEquals(SmartcardType.TIVU_HD, response.getSmartcardType());
        assertEquals(SmartcardSource.PHYSICAL, response.getSource());
        assertEquals(SmartcardStatus.PRELOADED, response.getStatus());
        assertEquals("TivuHD", response.getEntitlementId());
        assertEquals(PRODUCT_ID, response.getProductId());

        verify(nagraSmartcardGateway).preloadSmartcard(
                eq(SN),
                eq(SmartcardType.TIVU_HD),
                eq(SmartcardSource.PHYSICAL),
                eq(PRODUCT_ID),
                any(Instant.class),
                any(Instant.class)
        );
    }

    @Test
    void shouldFailWhenProductIdIsBlank() {
        PreloadSmartcardRequest request = new PreloadSmartcardRequest(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL,
                "   "
        );

        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                useCase.preload(request);
            }
        });

        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void shouldFailWhenSmartcardIsInLegacyTigerRange() {
        String tigerSn = smartcardValidator.buildSnFromUa("1092026368");

        PreloadSmartcardRequest request = new PreloadSmartcardRequest(
                tigerSn,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL,
                PRODUCT_ID
        );

        assertThrows(UnsupportedSmartcardRangeException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                useCase.preload(request);
            }
        });

        verifyNoInteractions(nagraSmartcardGateway);
    }
}