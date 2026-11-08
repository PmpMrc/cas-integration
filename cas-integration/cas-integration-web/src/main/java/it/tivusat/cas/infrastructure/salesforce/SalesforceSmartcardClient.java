package it.tivusat.cas.infrastructure.salesforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;

public class SalesforceSmartcardClient {

    private final SalesforceProperties properties;
    private final SalesforceTokenProvider tokenProvider;
    private final RestTemplate restTemplate;

    private final ObjectMapper errorMapper = new ObjectMapper();

    public SalesforceSmartcardClient(
            SalesforceProperties properties,
            SalesforceTokenProvider tokenProvider,
            RestTemplate restTemplate
    ) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;
        this.restTemplate = restTemplate;
    }

    public String findSmartcardType(String sn) {
        if (sn == null || !sn.matches("[0-9]{12}")) {
            throw new IllegalArgumentException(
                    "Smartcard serial must contain exactly 12 digits"
            );
        }

        SalesforceTokenResponse token = tokenProvider.getToken();

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return executeQuery(sn, token);
            } catch (RestClientResponseException ex) {
                if (attempt == 0 && isInvalidSession(ex)) {
                    token = tokenProvider.refreshAfterRejection(token);
                    continue;
                }

                SalesforceException.FailureType failureType =
                        ex.getRawStatusCode() == 401
                                ? SalesforceException.FailureType.AUTHENTICATION_ERROR
                                : SalesforceException.FailureType.QUERY_ERROR;

                throw new SalesforceException(
                        failureType,
                        "Salesforce smartcard query failed",
                        ex.getRawStatusCode()
                );
            } catch (ResourceAccessException ex) {
                throw new SalesforceException(
                        SalesforceException.FailureType.NETWORK_ERROR,
                        "Salesforce smartcard query could not be completed",
                        null
                );
            } catch (RestClientException ex) {
                throw invalidResponse();
            }
        }

        throw new IllegalStateException("Unexpected Salesforce query state");
    }

    private String executeQuery(
            String sn,
            SalesforceTokenResponse token
    ) {
        URI instanceUri =
                SalesforceUrlValidator.validate(token.getInstanceUrl());

        String instancePath = instanceUri.getPath();

        if (instancePath != null
                && !instancePath.isEmpty()
                && !"/".equals(instancePath)) {
            throw invalidResponse();
        }

        String query =
                "SELECT Id, Tipologia_Smart_Card__c "
                        + "FROM Smartcard__c "
                        + "WHERE Seriale_Smart_Card__c = '" + sn + "' "
                        + "LIMIT 2";

        URI uri = UriComponentsBuilder
                .fromUri(instanceUri)
                .replacePath(
                        "/services/data/"
                                + properties.getApiVersion()
                                + "/query"
                )
                .queryParam("q", "{query}")
                .encode()
                .buildAndExpand(query)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        headers.setAccept(
                Collections.singletonList(MediaType.APPLICATION_JSON)
        );

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<Void>(headers),
                JsonNode.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new SalesforceException(
                    SalesforceException.FailureType.QUERY_ERROR,
                    "Salesforce smartcard query returned an unexpected status",
                    response.getStatusCodeValue()
            );
        }

        return extractType(response.getBody());
    }

    private String extractType(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw invalidResponse();
        }

        JsonNode totalSize = body.get("totalSize");
        JsonNode done = body.get("done");
        JsonNode records = body.get("records");

        if (totalSize == null
                || !totalSize.isIntegralNumber()
                || !totalSize.canConvertToInt()
                || totalSize.intValue() < 0
                || done == null
                || !done.isBoolean()
                || records == null
                || !records.isArray()) {
            throw invalidResponse();
        }

        if (totalSize.intValue() > 1 || records.size() > 1) {
            throw new SalesforceException(
                    SalesforceException.FailureType.AMBIGUOUS_SMARTCARD,
                    "Salesforce returned multiple smartcards for the serial",
                    null
            );
        }

        // Con LIMIT 2 ci aspettiamo una risposta completa.
        // Non selezioniamo un risultato da una risposta incompleta.
        if (!done.booleanValue()
                || totalSize.intValue() != records.size()) {
            throw invalidResponse();
        }

        if (records.size() == 0) {
            throw new SalesforceException(
                    SalesforceException.FailureType.SMARTCARD_NOT_FOUND,
                    "Smartcard was not found in Salesforce",
                    null
            );
        }

        JsonNode record = records.get(0);

        if (!record.isObject()) {
            throw invalidResponse();
        }

        JsonNode type = record.get("Tipologia_Smart_Card__c");

        if (type == null
                || type.isNull()
                || (type.isTextual() && type.asText().trim().isEmpty())) {
            throw new SalesforceException(
                    SalesforceException.FailureType.SMARTCARD_TYPE_MISSING,
                    "Salesforce smartcard type is missing",
                    null
            );
        }

        if (!type.isTextual()) {
            throw invalidResponse();
        }

        return type.asText().trim();
    }

    private boolean isInvalidSession(RestClientResponseException ex) {
        if (ex.getRawStatusCode() != 401) {
            return false;
        }

        try {
            JsonNode errors =
                    errorMapper.readTree(ex.getResponseBodyAsString());

            if (errors == null || !errors.isArray()) {
                return false;
            }

            for (JsonNode error : errors) {
                if ("INVALID_SESSION_ID".equals(
                        error.path("errorCode").asText()
                )) {
                    return true;
                }
            }

            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private SalesforceException invalidResponse() {
        return new SalesforceException(
                SalesforceException.FailureType.INVALID_RESPONSE,
                "Salesforce returned an invalid smartcard query response",
                null
        );
    }
}