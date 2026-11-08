package it.tivusat.cas.infrastructure.salesforce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SalesforceConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(SalesforceConfig.class);

    @Test
    void shouldStartWithoutCredentialsWhenDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            SalesforceProperties properties =
                    context.getBean(SalesforceProperties.class);

            assertThat(properties.isEnabled()).isFalse();
        });
    }

    @Test
    void shouldBindConfigurationWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "salesforce.enabled=true",
                        "salesforce.token-url=http://localhost:9090/services/oauth2/token",
                        "salesforce.api-version=v60.0",
                        "salesforce.client-id=test-client",
                        "salesforce.client-secret=test-secret",
                        "salesforce.connect-timeout-ms=2000",
                        "salesforce.read-timeout-ms=4000"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    SalesforceProperties properties =
                            context.getBean(SalesforceProperties.class);

                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getTokenUrl()).isEqualTo(
                            "http://localhost:9090/services/oauth2/token"
                    );
                    assertThat(properties.getApiVersion()).isEqualTo("v60.0");
                    assertThat(properties.getClientId()).isEqualTo("test-client");
                    assertThat(properties.getClientSecret()).isEqualTo("test-secret");
                    assertThat(properties.getConnectTimeoutMs()).isEqualTo(2000);
                    assertThat(properties.getReadTimeoutMs()).isEqualTo(4000);
                });
    }

    @Test
    void shouldFailWhenEnabledWithoutClientSecret() {
        contextRunner
                .withPropertyValues(
                        "salesforce.enabled=true",
                        "salesforce.token-url=http://localhost:9090/services/oauth2/token",
                        "salesforce.api-version=v60.0",
                        "salesforce.client-id=test-client"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "salesforce.client-secret is required "
                                            + "when Salesforce is enabled"
                            );
                });
    }

    @Test
    void shouldFailWhenApiVersionHasInvalidFormat() {
        contextRunner
                .withPropertyValues(
                        "salesforce.enabled=true",
                        "salesforce.token-url=http://localhost:9090/services/oauth2/token",
                        "salesforce.api-version=60",
                        "salesforce.client-id=test-client",
                        "salesforce.client-secret=test-secret"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "salesforce.api-version must have format vNN.N"
                            );
                });
    }

    @Test
    void shouldFailWhenTimeoutIsNotPositive() {
        contextRunner
                .withPropertyValues(
                        "salesforce.enabled=true",
                        "salesforce.token-url=http://localhost:9090/services/oauth2/token",
                        "salesforce.api-version=v60.0",
                        "salesforce.client-id=test-client",
                        "salesforce.client-secret=test-secret",
                        "salesforce.read-timeout-ms=0"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "Salesforce timeouts must be greater than zero"
                            );
                });
    }
}