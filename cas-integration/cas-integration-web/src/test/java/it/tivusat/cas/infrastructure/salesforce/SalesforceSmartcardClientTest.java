package it.tivusat.cas.infrastructure.salesforce;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SalesforceSmartcardClientTest {

    private static final String SN = "109687603246";

    private SalesforceTokenProvider tokenProvider;
    private SalesforceTokenResponse token;
    private SalesforceSmartcardClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        SalesforceProperties properties = new SalesforceProperties();
        properties.setApiVersion("v60.0"); // Versione usata solo nel test.

        tokenProvider = mock(SalesforceTokenProvider.class);

        token = token("test-token");
        when(tokenProvider.getToken()).thenReturn(token);

        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        client = new SalesforceSmartcardClient(
                properties,
                tokenProvider,
                restTemplate
        );
    }

    @Test
    void shouldQueryBySerialAndReturnRawSalesforceType() {
        server.expect(request -> {
                    assertEquals(
                            "/services/data/v60.0/query",
                            request.getURI().getPath()
                    );

                    String decodedQuery = URLDecoder.decode(
                            request.getURI().getRawQuery(),
                            "UTF-8"
                    );

                    assertEquals(
                            "q=SELECT Id, Tipologia_Smart_Card__c "
                                    + "FROM Smartcard__c "
                                    + "WHERE Seriale_Smart_Card__c = '"
                                    + SN + "' LIMIT 2",
                            decodedQuery
                    );
                })
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess(
                        oneRecord("CRM_HD_TEST"),
                        MediaType.APPLICATION_JSON
                ));

        assertEquals("CRM_HD_TEST", client.findSmartcardType(SN));

        verify(tokenProvider, never()).refreshAfterRejection(any());
        server.verify();
    }

    @Test
    void shouldRejectMissingSmartcard() {
        server.expect(anything())
                .andRespond(withSuccess(
                        "{\"totalSize\":0,\"done\":true,\"records\":[]}",
                        MediaType.APPLICATION_JSON
                ));

        SalesforceException error = assertThrows(
                SalesforceException.class,
                () -> client.findSmartcardType(SN)
        );

        assertEquals(
                SalesforceException.FailureType.SMARTCARD_NOT_FOUND,
                error.getFailureType()
        );

        server.verify();
    }

    @Test
    void shouldRejectDuplicateSmartcards() {
        server.expect(anything())
                .andRespond(withSuccess(
                        "{\"totalSize\":2,\"done\":true,\"records\":["
                                + "{\"Id\":\"first\"},{\"Id\":\"second\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        SalesforceException error = assertThrows(
                SalesforceException.class,
                () -> client.findSmartcardType(SN)
        );

        assertEquals(
                SalesforceException.FailureType.AMBIGUOUS_SMARTCARD,
                error.getFailureType()
        );

        server.verify();
    }

    @Test
    void shouldRejectMissingType() {
        server.expect(anything())
                .andRespond(withSuccess(
                        "{\"totalSize\":1,\"done\":true,\"records\":["
                                + "{\"Id\":\"test\",\"Tipologia_Smart_Card__c\":null}]}",
                        MediaType.APPLICATION_JSON
                ));

        SalesforceException error = assertThrows(
                SalesforceException.class,
                () -> client.findSmartcardType(SN)
        );

        assertEquals(
                SalesforceException.FailureType.SMARTCARD_TYPE_MISSING,
                error.getFailureType()
        );

        server.verify();
    }

    @Test
    void shouldRefreshTokenOnceWhenSessionIsInvalid() throws Exception {
        SalesforceTokenResponse renewed = token("renewed-token");

        when(tokenProvider.refreshAfterRejection(token))
                .thenReturn(renewed);

        server.expect(anything())
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[{\"errorCode\":\"INVALID_SESSION_ID\"}]"));

        server.expect(anything())
                .andExpect(header("Authorization", "Bearer renewed-token"))
                .andRespond(withSuccess(
                        oneRecord("CRM_HD_TEST"),
                        MediaType.APPLICATION_JSON
                ));

        assertEquals("CRM_HD_TEST", client.findSmartcardType(SN));

        verify(tokenProvider, times(1)).refreshAfterRejection(token);
        server.verify();
    }

    @Test
    void shouldStopAfterSecondInvalidSession() throws Exception {
        SalesforceTokenResponse renewed = token("renewed-token");

        when(tokenProvider.refreshAfterRejection(token))
                .thenReturn(renewed);

        server.expect(anything())
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[{\"errorCode\":\"INVALID_SESSION_ID\"}]"));

        server.expect(anything())
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[{\"errorCode\":\"INVALID_SESSION_ID\"}]"));

        SalesforceException error = assertThrows(
                SalesforceException.class,
                () -> client.findSmartcardType(SN)
        );

        assertEquals(
                SalesforceException.FailureType.AUTHENTICATION_ERROR,
                error.getFailureType()
        );

        verify(tokenProvider, times(1)).refreshAfterRejection(token);
        server.verify();
    }

    @Test
    void shouldNotRefreshTokenForPermissionError() {
        server.expect(anything())
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[{\"errorCode\":\"INSUFFICIENT_ACCESS\"}]"));

        SalesforceException error = assertThrows(
                SalesforceException.class,
                () -> client.findSmartcardType(SN)
        );

        assertEquals(
                SalesforceException.FailureType.QUERY_ERROR,
                error.getFailureType()
        );

        verify(tokenProvider, never()).refreshAfterRejection(any());
        server.verify();
    }

    @Test
    void shouldRejectInvalidSerialBeforeRequestingToken() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.findSmartcardType("invalid' serial")
        );

        verifyNoInteractions(tokenProvider);
        server.verify();
    }

    private SalesforceTokenResponse token(String accessToken)
            throws Exception {
        return new ObjectMapper().readValue(
                "{\"access_token\":\"" + accessToken + "\","
                        + "\"instance_url\":\"https://example.my.salesforce.com\","
                        + "\"token_type\":\"Bearer\"}",
                SalesforceTokenResponse.class
        );
    }

    private String oneRecord(String type) {
        return "{\"totalSize\":1,\"done\":true,\"records\":["
                + "{\"Id\":\"test-record\","
                + "\"Tipologia_Smart_Card__c\":\"" + type + "\"}]}";
    }
}