package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.PreloadSmartcardRequest;
import it.tivusat.cas.domain.*;
import it.tivusat.cas.infrastructure.nagra.NagraSmartcardGateway;
import it.tivusat.cas.infrastructure.persistence.SmartcardEntity;
import it.tivusat.cas.infrastructure.persistence.SmartcardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreloadSmartcardUseCaseTest {

    private static final String SN = "109687603246";
    private static final String UA = "1096876032";
    private static final String PRODUCT_ID = "PRODUCT_TEST";

    @Mock
    private SmartcardRepository repository;

    @Mock
    private NagraSmartcardGateway nagraSmartcardGateway;

    private PreloadSmartcardUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PreloadSmartcardUseCase(
                repository,
                new SmartcardValidator(),
                nagraSmartcardGateway,
                new UaRangeClassifier()
        );
    }

    @Test
    void shouldPreloadNewSmartcard() {
        when(repository.findById(SN)).thenReturn(Optional.empty());

        PreloadSmartcardRequest request = new PreloadSmartcardRequest(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL,
                PRODUCT_ID
        );

        useCase.preload(request);

        ArgumentCaptor<SmartcardEntity> captor = ArgumentCaptor.forClass(SmartcardEntity.class);
        verify(repository).save(captor.capture());

        SmartcardEntity saved = captor.getValue();

        assertEquals(SN, saved.getSn());
        assertEquals(UA, saved.getUa());
        assertEquals(SmartcardType.TIVU_HD, saved.getSmartcardType());
        assertEquals(SmartcardSource.PHYSICAL, saved.getSource());
        assertEquals(SmartcardStatus.PRELOADED, saved.getStatus());
        assertTrue(saved.isAccountCreated());
        assertEquals("TivuHD", saved.getEntitlementId());
        assertEquals(PRODUCT_ID, saved.getProductId());
        assertNotNull(saved.getExpiryDate());

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
    void shouldPreloadExistingSmartcard() {
        SmartcardEntity existing = new SmartcardEntity(
                SN,
                UA,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        );

        when(repository.findById(SN)).thenReturn(Optional.of(existing));

        PreloadSmartcardRequest request = new PreloadSmartcardRequest(
                SN,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL,
                PRODUCT_ID
        );

        useCase.preload(request);

        assertEquals(SmartcardStatus.PRELOADED, existing.getStatus());
        assertTrue(existing.isAccountCreated());
        assertEquals("TivuHD", existing.getEntitlementId());
        assertEquals(PRODUCT_ID, existing.getProductId());
        assertNotNull(existing.getExpiryDate());

        verify(repository).save(existing);

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
    void shouldRejectInvalidSerialNumber() {
        PreloadSmartcardRequest request = new PreloadSmartcardRequest(
                "109687603200",
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL,
                PRODUCT_ID
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.preload(request)
        );

        assertTrue(exception.getMessage().contains("checksum"));

        verify(repository, never()).findById(anyString());
        verify(repository, never()).save(any());
        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void shouldUseVirtualSourceWhenPreloadingVirtualSmartcard() {
        when(repository.findById(SN)).thenReturn(Optional.empty());

        PreloadSmartcardRequest request = new PreloadSmartcardRequest(
                SN,
                SmartcardType.TIVU_VIRTUAL,
                SmartcardSource.VIRTUAL,
                PRODUCT_ID
        );

        useCase.preload(request);

        ArgumentCaptor<SmartcardEntity> captor = ArgumentCaptor.forClass(SmartcardEntity.class);
        verify(repository).save(captor.capture());

        SmartcardEntity saved = captor.getValue();

        assertEquals(SmartcardType.TIVU_VIRTUAL, saved.getSmartcardType());
        assertEquals(SmartcardSource.VIRTUAL, saved.getSource());
        assertEquals("TivuVirtual", saved.getEntitlementId());

        verify(nagraSmartcardGateway).preloadSmartcard(
                eq(SN),
                eq(SmartcardType.TIVU_VIRTUAL),
                eq(SmartcardSource.VIRTUAL),
                eq(PRODUCT_ID),
                any(Instant.class),
                any(Instant.class)
        );
    }

    @Test
    void shouldRejectTigerSmartcardRange() {
        PreloadSmartcardRequest request = new PreloadSmartcardRequest(
                "109202636869",
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL,
                PRODUCT_ID
        );

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> useCase.preload(request)
        );

        assertTrue(exception.getMessage().contains("legacy SOA/SMS"));

        verify(repository, never()).findById(anyString());
        verify(repository, never()).save(any());
        verifyNoInteractions(nagraSmartcardGateway);
    }
}
