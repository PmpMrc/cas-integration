package it.tivusat.cas.infrastructure.salesforce;

import org.springframework.stereotype.Component;

@Component
public class SalesforceTokenProvider {

    private final SalesforceProperties properties;
    private final SalesforceOAuthClient oauthClient;

    private SalesforceTokenResponse cachedToken;

    public SalesforceTokenProvider(
            SalesforceProperties properties,
            SalesforceOAuthClient oauthClient
    ) {
        this.properties = properties;
        this.oauthClient = oauthClient;
    }

    public synchronized SalesforceTokenResponse getToken() {
        checkEnabled();

        if (cachedToken == null) {
            cachedToken = oauthClient.requestToken();
        }

        return cachedToken;
    }

    public synchronized SalesforceTokenResponse refreshAfterRejection(
            SalesforceTokenResponse rejectedToken
    ) {
        checkEnabled();

        if (rejectedToken == null) {
            throw new IllegalArgumentException("rejectedToken is required");
        }

        /*
         * Un'altra richiesta potrebbe avere già sostituito il token.
         * In quel caso riutilizziamo quello nuovo.
         */
        if (cachedToken == rejectedToken) {
            cachedToken = null;
        }

        return getToken();
    }

    private void checkEnabled() {
        if (!properties.isEnabled()) {
            cachedToken = null;

            throw new SalesforceException(
                    SalesforceException.FailureType.INTEGRATION_DISABLED,
                    "Salesforce integration is disabled",
                    null
            );
        }
    }
}