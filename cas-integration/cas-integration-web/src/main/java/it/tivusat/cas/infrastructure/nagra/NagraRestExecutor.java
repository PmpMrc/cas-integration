package it.tivusat.cas.infrastructure.nagra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.tivusat.cas.application.NagraOperationLogService;
import it.tivusat.cas.domain.NagraOperation;
import it.tivusat.cas.domain.SmartcardSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NagraRestExecutor {

    private final RestTemplate restTemplate;
    private final NagraProperties properties;
    private final ObjectMapper objectMapper;
    private final NagraOperationLogService operationLogService;

    public NagraRestExecutor(
            RestTemplate restTemplate,
            NagraProperties properties,
            ObjectMapper objectMapper,
            NagraOperationLogService operationLogService
    ) {
        this.restTemplate = restTemplate;
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
        String url = buildUrl(endpoint);
        String requestPayload = serialize(body);
        long start = System.currentTimeMillis();

        try {
            HttpHeaders headers = buildHeaders(source, broadcastMode, body);
            HttpEntity<Object> entity = body == null
                    ? new HttpEntity<Object>(headers)
                    : new HttpEntity<Object>(body, headers);

            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    method,
                    entity,
                    responseType
            );

            long durationMs = System.currentTimeMillis() - start;
            String responsePayload = serialize(response.getBody());

            operationLogService.logSuccess(
                    smartcardSn,
                    operation,
                    method.name(),
                    endpoint,
                    requestPayload,
                    responsePayload,
                    response.getStatusCodeValue(),
                    durationMs
            );

            return response.getBody();

        } catch (HttpStatusCodeException exception) {
            long durationMs = System.currentTimeMillis() - start;
            String responseBody = exception.getResponseBodyAsString();

            operationLogService.logError(
                    smartcardSn,
                    operation,
                    method.name(),
                    endpoint,
                    requestPayload,
                    responseBody,
                    exception.getRawStatusCode(),
                    "HTTP_ERROR",
                    exception.getMessage(),
                    durationMs
            );

            throw new NagraException(
                    exception.getRawStatusCode(),
                    responseBody,
                    exception
            );
        } catch (ResourceAccessException exception) {
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
                    exception.getMessage(),
                    durationMs
            );

            throw new NagraException(
                    null,
                    exception.getMessage(),
                    exception
            );
        } catch (RestClientException exception) {
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
                    exception.getMessage(),
                    durationMs
            );

            throw new NagraException(
                    null,
                    exception.getMessage(),
                    exception
            );        }
    }

    private HttpHeaders buildHeaders(
            SmartcardSource source,
            String broadcastMode,
            Object body
    ) {
        HttpHeaders headers = new HttpHeaders();

        headers.set("nv-source-id", sourceIdFor(source));
        headers.set("nv-broadcast-mode", broadcastMode);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        return headers;
    }

    private String sourceIdFor(SmartcardSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Smartcard source is required");
        }

        switch (source) {
            case PHYSICAL:
                return properties.sourceId().physical();
            case VIRTUAL:
                return properties.sourceId().virtual();
            default:
                throw new IllegalArgumentException("Unsupported smartcard source: " + source);
        }
    }

    private String buildEndpoint(String uriTemplate, Object... uriVariables) {
        return UriComponentsBuilder
                .fromUriString(uriTemplate)
                .buildAndExpand(uriVariables)
                .toUriString();
    }

    private String buildUrl(String endpoint) {
        String baseUrl = properties.baseUrl();

        if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + endpoint;
        }

        if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
            return baseUrl + "/" + endpoint;
        }

        return baseUrl + endpoint;
    }

    private String serialize(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof String) {
            return (String) object;
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException exception) {
            return String.valueOf(object);
        }
    }
}