package it.tivusat.cas.infrastructure.salesforce;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesforceTokenProviderTest {

    @Mock
    private SalesforceOAuthClient oauthClient;

    private SalesforceProperties properties;
    private SalesforceTokenProvider provider;

    @BeforeEach
    void setUp() {
        properties = new SalesforceProperties();
        properties.setEnabled(true);

        provider = new SalesforceTokenProvider(properties, oauthClient);
    }

    @Test
    void shouldReuseTokenWithoutAuthenticatingAgain() throws Exception {
        SalesforceTokenResponse token = token("token-one");
        when(oauthClient.requestToken()).thenReturn(token);

        assertSame(token, provider.getToken());
        assertSame(token, provider.getToken());

        verify(oauthClient, times(1)).requestToken();
    }

    @Test
    void shouldReplaceRejectedToken() throws Exception {
        SalesforceTokenResponse first = token("token-one");
        SalesforceTokenResponse second = token("token-two");

        when(oauthClient.requestToken()).thenReturn(first, second);

        assertSame(first, provider.getToken());
        assertSame(second, provider.refreshAfterRejection(first));
        assertSame(second, provider.getToken());

        verify(oauthClient, times(2)).requestToken();
    }

    @Test
    void shouldReuseReplacementWhenOldTokenIsRejectedAgain() throws Exception {
        SalesforceTokenResponse first = token("token-one");
        SalesforceTokenResponse second = token("token-two");

        when(oauthClient.requestToken()).thenReturn(first, second);

        provider.getToken();
        provider.refreshAfterRejection(first);

        // Simula una seconda richiesta che aveva usato il vecchio token.
        assertSame(second, provider.refreshAfterRejection(first));

        verify(oauthClient, times(2)).requestToken();
    }

    @Test
    void shouldDiscardRejectedTokenEvenWhenRenewalFails() throws Exception {
        SalesforceTokenResponse first = token("token-one");
        SalesforceTokenResponse second = token("token-two");

        SalesforceException failure = new SalesforceException(
                SalesforceException.FailureType.NETWORK_ERROR,
                "Salesforce unavailable",
                null
        );

        when(oauthClient.requestToken())
                .thenReturn(first)
                .thenThrow(failure)
                .thenReturn(second);

        provider.getToken();

        SalesforceException actual = assertThrows(
                SalesforceException.class,
                () -> provider.refreshAfterRejection(first)
        );

        assertSame(failure, actual);

        // Una richiesta successiva deve autenticarsi di nuovo.
        assertSame(second, provider.getToken());

        verify(oauthClient, times(3)).requestToken();
    }

    @Test
    void shouldNotReturnCachedTokenWhenIntegrationIsDisabled() throws Exception {
        SalesforceTokenResponse token = token("token-one");
        when(oauthClient.requestToken()).thenReturn(token);

        provider.getToken();
        properties.setEnabled(false);

        SalesforceException exception = assertThrows(
                SalesforceException.class,
                () -> provider.getToken()
        );

        assertEquals(
                SalesforceException.FailureType.INTEGRATION_DISABLED,
                exception.getFailureType()
        );

        verify(oauthClient, times(1)).requestToken();
    }

    @Test
    void shouldRejectNullTokenWithoutAuthenticating() {
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.refreshAfterRejection(null)
        );

        verifyNoInteractions(oauthClient);
    }

    private SalesforceTokenResponse token(String value) throws Exception {
        return new ObjectMapper().readValue(
                "{"
                        + "\"access_token\":\"" + value + "\","
                        + "\"instance_url\":\"https://example.my.salesforce.com\","
                        + "\"token_type\":\"Bearer\""
                        + "}",
                SalesforceTokenResponse.class
        );
    }
}