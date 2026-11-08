package it.tivusat.cas.infrastructure.salesforce;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;

@Configuration
@EnableConfigurationProperties(SalesforceProperties.class)
public class SalesforceConfig {

    @Bean
    public SalesforceOAuthClient salesforceOAuthClient(
            SalesforceProperties properties
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory() {
                    @Override
                    protected void prepareConnection(
                            HttpURLConnection connection,
                            String httpMethod
                    ) throws IOException {
                        super.prepareConnection(connection, httpMethod);
                        connection.setInstanceFollowRedirects(false);
                    }
                };

        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        RestTemplate restTemplate = new RestTemplate(requestFactory);

        return new SalesforceOAuthClient(properties, restTemplate);
    }

    @Bean
    public SalesforceSmartcardClient salesforceSmartcardClient(
            SalesforceProperties properties,
            SalesforceTokenProvider tokenProvider
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory() {
                    @Override
                    protected void prepareConnection(
                            HttpURLConnection connection,
                            String httpMethod
                    ) throws IOException {
                        super.prepareConnection(connection, httpMethod);
                        connection.setInstanceFollowRedirects(false);
                    }
                };

        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        RestTemplate restTemplate = new RestTemplate(requestFactory);

        return new SalesforceSmartcardClient(
                properties,
                tokenProvider,
                restTemplate
        );
    }
}