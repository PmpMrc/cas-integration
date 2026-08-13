package it.tivusat.cas.application;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.infrastructure.persistence.OperationLogEntity;
import it.tivusat.cas.infrastructure.persistence.OperationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NagraOperationLogService {

    private final OperationLogRepository repository;

    public NagraOperationLogService(OperationLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(
            String smartcardSn,
            NagraOperation operation,
            String httpMethod,
            String endpoint,
            String requestPayload,
            String responsePayload,
            Integer httpStatus,
            Long durationMs
    ) {
        repository.save(OperationLogEntity.success(
                smartcardSn,
                operation,
                httpMethod,
                endpoint,
                requestPayload,
                responsePayload,
                httpStatus,
                durationMs
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(
            String smartcardSn,
            NagraOperation operation,
            String httpMethod,
            String endpoint,
            String requestPayload,
            String responsePayload,
            Integer httpStatus,
            String errorCode,
            String errorMessage,
            Long durationMs
    ) {
        repository.save(OperationLogEntity.error(
                smartcardSn,
                operation,
                httpMethod,
                endpoint,
                requestPayload,
                responsePayload,
                httpStatus,
                errorCode,
                errorMessage,
                durationMs
        ));
    }
}