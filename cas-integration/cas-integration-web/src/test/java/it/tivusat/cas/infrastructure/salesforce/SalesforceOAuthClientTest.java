package it.tivusat.cas.infrastructure.salesforce;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SalesforceOAuthClientTest {

    private static final String TOKEN_URL =
            "http://localhost:9090/services/oauth2/token";

    private SalesforceProperties properties;
    private MockRestServiceServer server;
    private SalesforceOAuthClient client;

    @BeforeEach
    void setUp() {
        properties = new SalesforceProperties();
        properties.setEnabled(true);
        properties.setTokenUrl(TOKEN_URL);
        properties.setApiVersion("v60.0");
        properties.setClientId("test-client");
        properties.setClientSecret("secret+with&symbols");

        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        client = new SalesforceOAuthClient(properties, restTemplate);
    }

    @Test
    void shouldRequestTokenUsingFormEncodedCredentials() {
        MultiValueMap<String, String> expectedForm =
                new LinkedMultiValueMap<>();

        expectedForm.add("grant_type", "client_credentials");
        expectedForm.add("client_id", "test-client");
        expectedForm.add("client_secret", "secret+with&symbols");

        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_FORM_URLENCODED
                ))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess(
                        "{"
                                + "\"access_token\":\"test-token\","
                                + "\"instance_url\":\"https://example.my.salesforce.com\","
                                + "\"token_type\":\"Bearer\","
                                + "\"issued_at\":\"1234567890\""
                                + "}",
                        MediaType.APPLICATION_JSON
                ));

        SalesforceTokenResponse response = client.requestToken();

        assertEquals("test-token", response.getAccessToken());
        assertEquals(
                "https://example.my.salesforce.com",
                response.getInstanceUrl()
        );
        server.verify();
    }

    @Test
    void shouldRejectRequestWhenDisabledWithoutHttpCall() {
        properties.setEnabled(false);

        SalesforceException exception = assertThrows(
                SalesforceException.class,
                () -> client.requestToken()
        );

        assertEquals(
                SalesforceException.FailureType.INTEGRATION_DISABLED,
                exception.getFailureType()
        );
        server.verify();
    }

    @Test
    void shouldNotExposeAuthenticationErrorBody() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_client\","
                                + "\"error_description\":\"sensitive-detail\"}"));

        SalesforceException exception = assertThrows(
                SalesforceException.class,
                () -> client.requestToken()
        );

        assertEquals(
                SalesforceException.FailureType.AUTHENTICATION_ERROR,
                exception.getFailureType()
        );
        assertEquals(
                Integer.valueOf(400),
                exception.getUpstreamHttpStatus()
        );
        assertFalse(exception.getMessage().contains("sensitive-detail"));
        assertNull(exception.getCause());
        server.verify();
    }

    @Test
    void shouldRejectResponseWithoutAccessToken() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess(
                        "{\"instance_url\":\"https://example.my.salesforce.com\","
                                + "\"token_type\":\"Bearer\"}",
                        MediaType.APPLICATION_JSON
                ));

        SalesforceException exception = assertThrows(
                SalesforceException.class,
                () -> client.requestToken()
        );

        assertEquals(
                SalesforceException.FailureType.INVALID_RESPONSE,
                exception.getFailureType()
        );
        server.verify();
    }

    @Test
    void shouldRejectNonLocalHttpTokenUrlWithoutHttpCall() {
        properties.setTokenUrl(
                "http://example.my.salesforce.com/services/oauth2/token"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> client.requestToken()
        );
        server.verify();
    }
}