package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ActivateSmartcardRequest;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.domain.SmartcardType;
import it.tivusat.cas.infrastructure.nagra.NagraSmartcardGateway;
import it.tivusat.cas.infrastructure.persistence.SmartcardEntity;
import it.tivusat.cas.infrastructure.persistence.SmartcardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.tivusat.cas.domain.exception.SmartcardNotFoundException;

@Service
public class SmartcardOperationsUseCase {

    private final SmartcardRepository repository;
    private final NagraSmartcardGateway nagraSmartcardGateway;

    public SmartcardOperationsUseCase(
            SmartcardRepository repository,
            NagraSmartcardGateway nagraSmartcardGateway
    ) {
        this.repository = repository;
        this.nagraSmartcardGateway = nagraSmartcardGateway;
    }

    @Transactional
    public SmartcardEntity activate(String sn, ActivateSmartcardRequest request) {
        SmartcardEntity smartcard = findSmartcard(sn);

        if (smartcard.getStatus() == SmartcardStatus.DELETED) {
            throw new IllegalStateException("Cannot activate a deleted smartcard");
        }

        validateCaSnIfRequired(smartcard, request.caSn());

        nagraSmartcardGateway.activateSmartcard(
                smartcard.getSn(),
                smartcard.getUa(),
                smartcard.getSource(),
                request.caSn()
        );

        smartcard.markEnabled(request.caSn());
        return repository.save(smartcard);
    }

    @Transactional
    public SmartcardEntity refresh(String sn, ActivateSmartcardRequest request) {
        SmartcardEntity smartcard = findSmartcard(sn);

        if (smartcard.getStatus() == SmartcardStatus.DELETED) {
            throw new IllegalStateException("Cannot refresh a deleted smartcard");
        }

        validateCaSnIfRequired(smartcard, request.caSn());

        nagraSmartcardGateway.refreshSmartcard(
                smartcard.getSn(),
                smartcard.getUa(),
                smartcard.getSource(),
                request.caSn()
        );

        smartcard.markRefreshed(request.caSn());
        return repository.save(smartcard);
    }

    @Transactional
    public SmartcardEntity suspend(String sn) {
        SmartcardEntity smartcard = findSmartcard(sn);

        if (smartcard.getStatus() == SmartcardStatus.DELETED) {
            throw new IllegalStateException("Cannot suspend a deleted smartcard");
        }

        nagraSmartcardGateway.suspendSmartcard(
                smartcard.getSn(),
                smartcard.getSource()
        );

        smartcard.markDisabled();
        return repository.save(smartcard);
    }

    @Transactional
    public SmartcardEntity delete(String sn) {
        SmartcardEntity smartcard = findSmartcard(sn);

        nagraSmartcardGateway.deleteSmartcard(
                smartcard.getSn(),
                smartcard.getSource()
        );

        smartcard.markDeleted();
        return repository.save(smartcard);
    }

    @Transactional(readOnly = true)
    public SmartcardEntity getStatus(String sn) {
        return findSmartcard(sn);
    }

    private SmartcardEntity findSmartcard(String sn) {
        return repository.findById(sn)
                .orElseThrow(() -> new SmartcardNotFoundException(sn));
    }

    private void validateCaSnIfRequired(SmartcardEntity smartcard, String caSn) {
        if (smartcard.getSmartcardType() == SmartcardType.TIVU_HD_PAIRING
                && (caSn == null || caSn.isBlank())) {
            throw new IllegalArgumentException("caSn is mandatory for TIVU_HD_PAIRING smartcards");
        }
    }
}