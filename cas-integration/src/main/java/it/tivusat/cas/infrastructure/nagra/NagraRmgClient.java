package it.tivusat.cas.infrastructure.nagra;

import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class NagraRmgClient {

    private final RestClient restClient;
    private final NagraProperties properties;

    public NagraRmgClient(
            RestClient nagraRestClient,
            NagraProperties properties
    ) {
        this.restClient = nagraRestClient;
        this.properties = properties;
    }

    public void createEntitlement(
            SmartcardSource source,
            RmgEntitlementRequest request
    ) {
        restClient.post()
                .uri(properties.paths().createEntitlement())
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