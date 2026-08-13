package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.infrastructure.nagra.dto.AdmAccountRequest;
import it.tivusat.cas.infrastructure.nagra.dto.AdmDeviceRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class NagraAdmClient {

    private final RestClient restClient;
    private final NagraProperties properties;

    public NagraAdmClient(
            RestClient nagraRestClient,
            NagraProperties properties
    ) {
        this.restClient = nagraRestClient;
        this.properties = properties;
    }

    public void createAccount(
            SmartcardSource source,
            AdmAccountRequest request
    ) {
        restClient.post()
                .uri(properties.paths().createAccount())
                .headers(headers -> applyHeaders(headers, source, "W"))
                .body(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (requestSpec, response) -> {
                            throw toNagraException(response);
                        }
                )
                .toBodilessEntity();
    }

    public void createDevice(
            SmartcardSource source,
            AdmDeviceRequest request
    ) {
        restClient.post()
                .uri(properties.paths().createDevice())
                .headers(headers -> applyHeaders(headers, source, "N"))
                .body(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (requestSpec, response) -> {
                            throw toNagraException(response);
                        }
                )
                .toBodilessEntity();
    }

    public void updateDeviceForRefresh(
            SmartcardSource source,
            String sn,
            AdmDeviceRequest request
    ) {
        restClient.put()
                .uri(properties.paths().updateDevice(), sn)
                .headers(headers -> applyHeaders(headers, source, "1"))
                .body(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (requestSpec, response) -> {
                            throw toNagraException(response);
                        }
                )
                .toBodilessEntity();
    }

    public void suspendDevice(
            SmartcardSource source,
            String sn
    ) {
        restClient.put()
                .uri(properties.paths().updateDevice(), sn)
                .headers(headers -> applyHeaders(headers, source, "1"))
                .body(AdmDeviceRequest.suspend())
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (requestSpec, response) -> {
                            throw toNagraException(response);
                        }
                )
                .toBodilessEntity();
    }

    public void deleteDevice(
            SmartcardSource source,
            String sn
    ) {
        restClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path(properties.paths().updateDevice())
                        .queryParam("cancelICC", true)
                        .build(sn)
                )
                .headers(headers -> applyHeaders(headers, source, "1"))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (requestSpec, response) -> {
                            throw toNagraException(response);
                        }
                )
                .toBodilessEntity();
    }

    private void applyHeaders(
            HttpHeaders headers,
            SmartcardSource source,
            String broadcastMode
    ) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("nv-source-id", resolveSourceId(source));
        headers.set("nv-broadcast-mode", broadcastMode);
    }

    private String resolveSourceId(SmartcardSource source) {
        return switch (source) {
            case PHYSICAL -> properties.sourceId().physical();
            case VIRTUAL -> properties.sourceId().virtual();
        };
    }

    private NagraException toNagraException(
            org.springframework.http.client.ClientHttpResponse response
    ) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        return new NagraException(response.getStatusCode().value(), body);
    }
}