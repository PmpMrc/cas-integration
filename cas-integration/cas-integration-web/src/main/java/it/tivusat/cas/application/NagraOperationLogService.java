package it.tivusat.cas.application;

import it.tivusat.cas.domain.NagraOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NagraOperationLogService {

    private static final Logger log = LoggerFactory.getLogger(NagraOperationLogService.class);

    public void logSuccess(
            String smartcardSn,
            NagraOperation operation,
            String httpMethod,
            String endpoint,
            String requestPayload,
            String responsePayload,
            Integer httpStatus,
            long durationMs
    ) {
        log.info(
                "NAGRA SUCCESS smartcardSn={} operation={} method={} endpoint={} httpStatus={} durationMs={} requestPayload={} responsePayload={}",
                smartcardSn,
                operation,
                httpMethod,
                endpoint,
                httpStatus,
                durationMs,
                requestPayload,
                responsePayload
        );
    }

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
            long durationMs
    ) {
        log.error(
                "NAGRA ERROR smartcardSn={} operation={} method={} endpoint={} httpStatus={} errorCode={} durationMs={} requestPayload={} responsePayload={} errorMessage={}",
                smartcardSn,
                operation,
                httpMethod,
                endpoint,
                httpStatus,
                errorCode,
                durationMs,
                requestPayload,
                responsePayload,
                errorMessage
        );
    }
}