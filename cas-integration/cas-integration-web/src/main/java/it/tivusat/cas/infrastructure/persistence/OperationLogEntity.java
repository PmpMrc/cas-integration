package it.tivusat.cas.infrastructure.persistence;

import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.OperationResult;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nagra_operation_log")
public class OperationLogEntity {

    @Id
    private UUID id;

    private String smartcardSn;

    @Enumerated(EnumType.STRING)
    private NagraOperation operation;

    private String httpMethod;

    private String endpoint;

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    private Integer httpStatus;

    @Enumerated(EnumType.STRING)
    private OperationResult result;

    private String errorCode;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Long durationMs;

    private Instant createdAt;

    protected OperationLogEntity() {
    }

    private OperationLogEntity(
            UUID id,
            String smartcardSn,
            NagraOperation operation,
            String httpMethod,
            String endpoint,
            String requestPayload,
            String responsePayload,
            Integer httpStatus,
            OperationResult result,
            String errorCode,
            String errorMessage,
            Long durationMs,
            Instant createdAt
    ) {
        this.id = id;
        this.smartcardSn = smartcardSn;
        this.operation = operation;
        this.httpMethod = httpMethod;
        this.endpoint = endpoint;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.httpStatus = httpStatus;
        this.result = result;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.createdAt = createdAt;
    }

    public static OperationLogEntity success(
            String smartcardSn,
            NagraOperation operation,
            String httpMethod,
            String endpoint,
            String requestPayload,
            String responsePayload,
            Integer httpStatus,
            Long durationMs
    ) {
        return new OperationLogEntity(
                UUID.randomUUID(),
                smartcardSn,
                operation,
                httpMethod,
                endpoint,
                requestPayload,
                responsePayload,
                httpStatus,
                OperationResult.SUCCESS,
                null,
                null,
                durationMs,
                Instant.now()
        );
    }

    public static OperationLogEntity error(
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
        return new OperationLogEntity(
                UUID.randomUUID(),
                smartcardSn,
                operation,
                httpMethod,
                endpoint,
                requestPayload,
                responsePayload,
                httpStatus,
                OperationResult.ERROR,
                errorCode,
                errorMessage,
                durationMs,
                Instant.now()
        );
    }
}