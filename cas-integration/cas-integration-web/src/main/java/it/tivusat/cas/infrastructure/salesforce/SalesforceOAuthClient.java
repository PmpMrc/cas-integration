package it.tivusat.cas.infrastructure.salesforce;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;

public class SalesforceOAuthClient {

    private final SalesforceProperties properties;
    private final RestTemplate restTemplate;

    public SalesforceOAuthClient(
            SalesforceProperties properties,
            RestTemplate restTemplate
    ) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public SalesforceTokenResponse requestToken() {
        if (!properties.isEnabled()) {
            throw new SalesforceException(
                    SalesforceException.FailureType.INTEGRATION_DISABLED,
                    "Salesforce integration is disabled",
                    null
            );
        }

        URI tokenUri =
                SalesforceUrlValidator.validate(properties.getTokenUrl());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        ResponseEntity<SalesforceTokenResponse> response;

        try {
            response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    new HttpEntity<>(form, headers),
                    SalesforceTokenResponse.class
            );
        } catch (RestClientResponseException exception) {
            throw new SalesforceException(
                    SalesforceException.FailureType.AUTHENTICATION_ERROR,
                    "Salesforce token request failed",
                    exception.getRawStatusCode()
            );
        } catch (ResourceAccessException exception) {
            throw new SalesforceException(
                    SalesforceException.FailureType.NETWORK_ERROR,
                    "Salesforce token endpoint could not be reached",
                    null
            );
        } catch (RestClientException exception) {
            throw invalidResponse();
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new SalesforceException(
                    SalesforceException.FailureType.AUTHENTICATION_ERROR,
                    "Salesforce token endpoint returned an unexpected HTTP status",
                    response.getStatusCodeValue()
            );
        }

        SalesforceTokenResponse token = response.getBody();

        if (token == null
                || isBlank(token.getAccessToken())
                || isBlank(token.getInstanceUrl())
                || !"Bearer".equalsIgnoreCase(token.getTokenType())) {
            throw invalidResponse();
        }

        try {
            SalesforceUrlValidator.validate(token.getInstanceUrl());
        } catch (IllegalArgumentException exception) {
            throw invalidResponse();
        }

        return token;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private SalesforceException invalidResponse() {
        return new SalesforceException(
                SalesforceException.FailureType.INVALID_RESPONSE,
                "Salesforce returned an invalid token response",
                null
        );
    }
}