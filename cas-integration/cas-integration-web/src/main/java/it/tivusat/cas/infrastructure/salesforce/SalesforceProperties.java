package it.tivusat.cas.infrastructure.salesforce;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import it.tivusat.cas.domain.SmartcardType;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "salesforce")
public class SalesforceProperties implements InitializingBean {

    private boolean enabled;
    private String tokenUrl;
    private String apiVersion;
    private String clientId;
    private String clientSecret;

    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 15000;
    private Map<String, SmartcardType> typeMapping = new LinkedHashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (!enabled) {
            return;
        }

        requireConfigured(tokenUrl, "salesforce.token-url");
        requireConfigured(apiVersion, "salesforce.api-version");
        requireConfigured(clientId, "salesforce.client-id");
        requireConfigured(clientSecret, "salesforce.client-secret");
        SalesforceUrlValidator.validate(tokenUrl);

        if (!apiVersion.matches("v\\d+\\.\\d+")) {
            throw new IllegalStateException(
                    "salesforce.api-version must have format vNN.N"
            );
        }

        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalStateException(
                    "Salesforce timeouts must be greater than zero"
            );
        }
    }

    private void requireConfigured(String value, String propertyName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    propertyName + " is required when Salesforce is enabled"
            );
        }
    }

    public Map<String, SmartcardType> getTypeMapping() {
        return typeMapping;
    }

    public void setTypeMapping(Map<String, SmartcardType> typeMapping) {
        this.typeMapping = typeMapping == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(typeMapping);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}