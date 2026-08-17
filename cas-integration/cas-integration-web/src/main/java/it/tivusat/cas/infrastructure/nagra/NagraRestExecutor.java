package it.tivusat.cas.infrastructure.nagra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.tivusat.cas.application.NagraOperationLogService;
import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NagraRestExecutor {

    private final RestClient restClient;
    private final NagraProperties properties;
    private final ObjectMapper objectMapper;
    private final NagraOperationLogService operationLogService;

    public NagraRestExecutor(
            RestClient nagraRestClient,
            NagraProperties properties,
            ObjectMapper objectMapper,
            NagraOperationLogService operationLogService
    ) {
        this.restClient = nagraRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.operationLogService = operationLogService;
    }

    public void execute(
            NagraOperation operation,
            String smartcardSn,
            HttpMethod method,
            String uriTemplate,
            SmartcardSource source,
            String broadcastMode,
            Object body,
            Object... uriVariables
    ) {
        if (HttpMethod.DELETE.equals(method)) {
            execute(
                    operation,
                    smartcardSn,
                    method,
                    uriTemplate,
                    source,
                    broadcastMode,
                    body,
                    Void.class,
                    uriVariables
            );
            return;
        }

        execute(
                operation,
                smartcardSn,
                method,
                uriTemplate,
                source,
                broadcastMode,
                body,
                String.class,
                uriVariables
        );
    }

    public <T> T execute(
            NagraOperation operation,
            String smartcardSn,
            HttpMethod method,
            String uriTemplate,
            SmartcardSource source,
            String broadcastMode,
            Object body,
            Class<T> responseType,
            Object... uriVariables
    ) {
        String endpoint = buildEndpoint(uriTemplate, uriVariables);
        String requestPayload = toPayload(body);
        long start = System.currentTimeMillis();

        try {
            RestClient.RequestBodySpec requestSpec = restClient
                    .method(method)
                    .uri(uriTemplate, uriVariables)
                    .headers(headers -> applyHeaders(headers, source, broadcastMode, body));

            RestClient.RequestHeadersSpec<?> headersSpec = body != null
                    ? requestSpec.body(body)
                    : requestSpec;

            ResponseEntity<T> response;

            if (Void.class.equals(responseType)) {
                ResponseEntity<Void> bodilessResponse = headersSpec
                        .retrieve()
                        .toBodilessEntity();

                response = new ResponseEntity<>(
                        null,
                        bodilessResponse.getHeaders(),
                        bodilessResponse.getStatusCode()
                );
            } else {
                response = headersSpec
                        .retrieve()
                        .toEntity(responseType);
            }

            long durationMs = System.currentTimeMillis() - start;

            operationLogService.logSuccess(
                    smartcardSn,
                    operation,
                    method.name(),
                    endpoint,
                    requestPayload,
                    toPayload(response.getBody()),
                    response.getStatusCode().value(),
                    durationMs
            );

            return response.getBody();

        } catch (RestClientResponseException ex) {
            long durationMs = System.currentTimeMillis() - start;
            String responseBody = ex.getResponseBodyAsString();

            operationLogService.logError(
                    smartcardSn,
                    operation,
                    method.name(),
                    endpoint,
                    requestPayload,
                    responseBody,
                    ex.getStatusCode().value(),
                    null,
                    ex.getMessage(),
                    durationMs
            );

            throw new NagraException(
                    ex.getStatusCode().value(),
                    responseBody,
                    ex
            );

        } catch (RestClientException ex) {
            long durationMs = System.currentTimeMillis() - start;

            operationLogService.logError(
                    smartcardSn,
                    operation,
                    method.name(),
                    endpoint,
                    requestPayload,
                    null,
                    null,
                    "TECHNICAL_ERROR",
                    ex.getMessage(),
                    durationMs
            );

            throw new NagraException(
                    null,
                    ex.getMessage(),
                    ex
            );
        }
    }

    private void applyHeaders(
            HttpHeaders headers,
            SmartcardSource source,
            String broadcastMode,
            Object body
    ) {
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        headers.set("nv-source-id", resolveSourceId(source));
        headers.set("nv-broadcast-mode", broadcastMode);
    }

    private String resolveSourceId(SmartcardSource source) {
        return switch (source) {
            case PHYSICAL -> properties.sourceId().physical();
            case VIRTUAL -> properties.sourceId().virtual();
        };
    }

    private String buildEndpoint(String uriTemplate, Object... uriVariables) {
        return UriComponentsBuilder
                .fromUriString(uriTemplate)
                .buildAndExpand(uriVariables)
                .toUriString();
    }

    private String toPayload(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String stringValue) {
            return stringValue;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "<unable to serialize payload>";
        }
    }
}