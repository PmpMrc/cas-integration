package it.tivusat.cas.application;

import it.tivusat.cas.api.dto.ActivateSmartcardRequest;
import it.tivusat.cas.domain.SmartcardStatus;
import it.tivusat.cas.infrastructure.persistence.SmartcardEntity;
import it.tivusat.cas.infrastructure.persistence.SmartcardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmartcardOperationsUseCase {

    private final SmartcardRepository repository;

    public SmartcardOperationsUseCase(SmartcardRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SmartcardEntity activate(String sn, ActivateSmartcardRequest request) {
        SmartcardEntity smartcard = findSmartcard(sn);

        if (smartcard.getStatus() == SmartcardStatus.DELETED) {
            throw new IllegalStateException("Cannot activate a deleted smartcard");
        }

        smartcard.markEnabled(request.caSn());
        return repository.save(smartcard);
    }

    @Transactional
    public SmartcardEntity refresh(String sn, ActivateSmartcardRequest request) {
        SmartcardEntity smartcard = findSmartcard(sn);

        if (smartcard.getStatus() == SmartcardStatus.DELETED) {
            throw new IllegalStateException("Cannot refresh a deleted smartcard");
        }

        smartcard.markRefreshed(request.caSn());
        return repository.save(smartcard);
    }

    @Transactional
    public SmartcardEntity suspend(String sn) {
        SmartcardEntity smartcard = findSmartcard(sn);

        if (smartcard.getStatus() == SmartcardStatus.DELETED) {
            throw new IllegalStateException("Cannot suspend a deleted smartcard");
        }

        smartcard.markDisabled();
        return repository.save(smartcard);
    }

    @Transactional
    public SmartcardEntity delete(String sn) {
        SmartcardEntity smartcard = findSmartcard(sn);
        smartcard.markDeleted();
        return repository.save(smartcard);
    }

    @Transactional(readOnly = true)
    public SmartcardEntity getStatus(String sn) {
        return findSmartcard(sn);
    }

    private SmartcardEntity findSmartcard(String sn) {
        return repository.findById(sn)
                .orElseThrow(() -> new IllegalArgumentException("Smartcard not found: " + sn));
    }
}