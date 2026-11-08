package it.tivusat.cas.infrastructure.salesforce;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalesforceTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("instance_url")
    private String instanceUrl;

    @JsonProperty("token_type")
    private String tokenType;

    public SalesforceTokenResponse() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public String getTokenType() {
        return tokenType;
    }
}