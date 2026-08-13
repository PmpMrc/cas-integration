package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ActivateSmartcardRequest;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.NagraSmartcardGateway;
import it.tivusat.cas.infrastructure.persistence.SmartcardEntity;
import it.tivusat.cas.infrastructure.persistence.SmartcardRepository;
import it.tivusat.cas.domain.exception.SmartcardNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartcardOperationsUseCaseTest {

    private static final String SN = "109687603246";
    private static final String UA = "1096876032";
    private static final String PRODUCT_ID = "PRODUCT_TEST";
    private static final String CA_SN = "1234567890";

    @Mock
    private SmartcardRepository repository;

    @Mock
    private NagraSmartcardGateway nagraSmartcardGateway;

    private SmartcardOperationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SmartcardOperationsUseCase(
                repository,
                nagraSmartcardGateway
        );
    }

    @Test
    void shouldActivateSmartcard() {
        SmartcardEntity smartcard = createPreloadedSmartcard();

        givenExistingSmartcard(smartcard);
        givenSaveReturnsArgument();

        SmartcardEntity result = useCase.activate(
                SN,
                new ActivateSmartcardRequest(null)
        );

        assertEquals(SmartcardStatus.ENABLED, result.getStatus());
        assertTrue(result.isDeviceCreated());
        assertNull(result.getCaSn());

        verify(nagraSmartcardGateway).activateSmartcard(
                SN,
                UA,
                SmartcardSource.PHYSICAL,
                null
        );

        verify(repository).save(smartcard);
    }

    @Test
    void shouldRefreshSmartcard() {
        SmartcardEntity smartcard = createPreloadedSmartcard();
        smartcard.markEnabled(null);

        givenExistingSmartcard(smartcard);
        givenSaveReturnsArgument();

        SmartcardEntity result = useCase.refresh(
                SN,
                new ActivateSmartcardRequest(null)
        );

        assertEquals(SmartcardStatus.ENABLED, result.getStatus());

        verify(nagraSmartcardGateway).refreshSmartcard(
                SN,
                UA,
                SmartcardSource.PHYSICAL,
                null
        );

        verify(repository).save(smartcard);
    }

    @Test
    void shouldSuspendSmartcard() {
        SmartcardEntity smartcard = createPreloadedSmartcard();
        smartcard.markEnabled(null);

        givenExistingSmartcard(smartcard);
        givenSaveReturnsArgument();

        SmartcardEntity result = useCase.suspend(SN);

        assertEquals(SmartcardStatus.DISABLED, result.getStatus());

        verify(nagraSmartcardGateway).suspendSmartcard(
                SN,
                SmartcardSource.PHYSICAL
        );

        verify(repository).save(smartcard);
    }

    @Test
    void shouldDeleteSmartcard() {
        SmartcardEntity smartcard = createPreloadedSmartcard();

        givenExistingSmartcard(smartcard);
        givenSaveReturnsArgument();

        SmartcardEntity result = useCase.delete(SN);

        assertEquals(SmartcardStatus.DELETED, result.getStatus());

        verify(nagraSmartcardGateway).deleteSmartcard(
                SN,
                SmartcardSource.PHYSICAL
        );

        verify(repository).save(smartcard);
    }

    @Test
    void shouldGetSmartcardStatus() {
        SmartcardEntity smartcard = createPreloadedSmartcard();

        givenExistingSmartcard(smartcard);

        SmartcardEntity result = useCase.getStatus(SN);

        assertEquals(SN, result.getSn());
        assertEquals(UA, result.getUa());
        assertEquals(SmartcardStatus.PRELOADED, result.getStatus());

        verify(repository).findById(SN);
        verify(repository, never()).save(any());
        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void shouldThrowExceptionWhenSmartcardDoesNotExist() {
        when(repository.findById(SN)).thenReturn(Optional.empty());

        SmartcardNotFoundException exception = assertThrows(
                SmartcardNotFoundException.class,
                () -> useCase.getStatus(SN)
        );

        assertTrue(exception.getMessage().contains("Smartcard not found"));

        verify(repository).findById(SN);
        verify(repository, never()).save(any());
        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void shouldNotActivateDeletedSmartcard() {
        SmartcardEntity smartcard = createPreloadedSmartcard();
        smartcard.markDeleted();

        givenExistingSmartcard(smartcard);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> useCase.activate(SN, new ActivateSmartcardRequest(null))
        );

        assertTrue(exception.getMessage().contains("deleted"));

        verify(repository).findById(SN);
        verify(repository, never()).save(any());
        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void shouldNotRefreshDeletedSmartcard() {
        SmartcardEntity smartcard = createPreloadedSmartcard();
        smartcard.markDeleted();

        givenExistingSmartcard(smartcard);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> useCase.refresh(SN, new ActivateSmartcardRequest(null))
        );

        assertTrue(exception.getMessage().contains("deleted"));

        verify(repository).findById(SN);
        verify(repository, never()).save(any());
        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void shouldNotSuspendDeletedSmartcard() {
        SmartcardEntity smartcard = createPreloadedSmartcard();
        smartcard.markDeleted();

        givenExistingSmartcard(smartcard);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> useCase.suspend(SN)
        );

        assertTrue(exception.getMessage().contains("deleted"));

        verify(repository).findById(SN);
        verify(repository, never()).save(any());
        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void shouldRequireCaSnForPairingSmartcardOnActivate() {
        SmartcardEntity smartcard = createPairingSmartcard();

        givenExistingSmartcard(smartcard);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.activate(SN, new ActivateSmartcardRequest(null))
        );

        assertTrue(exception.getMessage().contains("caSn"));

        verify(repository).findById(SN);
        verify(repository, never()).save(any());
        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void shouldRequireCaSnForPairingSmartcardOnRefresh() {
        SmartcardEntity smartcard = createPairingSmartcard();

        givenExistingSmartcard(smartcard);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.refresh(SN, new ActivateSmartcardRequest(""))
        );

        assertTrue(exception.getMessage().contains("caSn"));

        verify(repository).findById(SN);
        verify(repository, never()).save(any());
        verifyNoInteractions(nagraSmartcardGateway);
    }

    @Test
    void shouldActivatePairingSmartcardWithCaSn() {
        SmartcardEntity smartcard = createPairingSmartcard();

        givenExistingSmartcard(smartcard);
        givenSaveReturnsArgument();

        SmartcardEntity result = useCase.activate(
                SN,
                new ActivateSmartcardRequest(CA_SN)
        );

        assertEquals(SmartcardStatus.ENABLED, result.getStatus());
        assertEquals(CA_SN, result.getCaSn());

        verify(nagraSmartcardGateway).activateSmartcard(
                SN,
                UA,
                SmartcardSource.PHYSICAL,
                CA_SN
        );

        verify(repository).save(smartcard);
    }

    private void givenExistingSmartcard(SmartcardEntity smartcard) {
        when(repository.findById(SN)).thenReturn(Optional.of(smartcard));
    }

    private void givenSaveReturnsArgument() {
        when(repository.save(any(SmartcardEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private SmartcardEntity createPreloadedSmartcard() {
        SmartcardEntity smartcard = new SmartcardEntity(
                SN,
                UA,
                SmartcardType.TIVU_HD,
                SmartcardSource.PHYSICAL
        );

        smartcard.markPreloaded(
                SmartcardType.TIVU_HD.getNagraType(),
                PRODUCT_ID,
                Instant.now().plusSeconds(60L * 60 * 24 * 365 * 4)
        );

        return smartcard;
    }

    private SmartcardEntity createPairingSmartcard() {
        SmartcardEntity smartcard = new SmartcardEntity(
                SN,
                UA,
                SmartcardType.TIVU_HD_PAIRING,
                SmartcardSource.PHYSICAL
        );

        smartcard.markPreloaded(
                SmartcardType.TIVU_HD_PAIRING.getNagraType(),
                PRODUCT_ID,
                Instant.now().plusSeconds(60L * 60 * 24 * 365 * 4)
        );

        return smartcard;
    }
}