package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.NagraOperation;

public class NagraPartialFailureException extends NagraException {

    private final NagraOperation completedOperation;

    public NagraPartialFailureException(
            NagraOperation completedOperation,
            NagraException failure
    ) {
        super(
                failure.getHttpStatus(),
                failure.getResponseBody(),
                failure,
                failure.getOperation(),
                failure.getEndpoint(),
                failure.getFailureType(),
                failure.getRequestPayload()
        );
        this.completedOperation = completedOperation;
    }

    public NagraOperation getCompletedOperation() {
        return completedOperation;
    }
}