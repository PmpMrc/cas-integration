package it.tivusat.cas.infrastructure.nagra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.tivusat.cas.application.NagraOperationLogService;
import it.tivusat.cas.domain.SmartcardSource;
import it.tivusat.cas.infrastructure.nagra.dto.RmgEntitlementRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class NagraClientConfigTest {

    @Test
    void sendsIsoDatesInTheActualRmgHttpBody() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        RestTemplate restTemplate = new NagraClientConfig().nagraRestTemplate(objectMapper);
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://localhost:9090/rmg/v1/operator/entitlements"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.validFrom").value("2026-09-24T08:44:52Z"))
                .andExpect(jsonPath("$.expiryDate").value("2030-09-24T08:44:52Z"))
                .andRespond(withSuccess("{\"result\":\"created\"}", MediaType.APPLICATION_JSON));

        NagraProperties properties = new NagraProperties();
        properties.setBaseUrl("http://localhost:9090");
        properties.getSourceId().setPhysical("1000");
        properties.getPaths().setCreateEntitlement("/rmg/v1/operator/entitlements");
        NagraRestExecutor executor = new NagraRestExecutor(
                restTemplate, properties, objectMapper, new NagraOperationLogService());
        new NagraRmgClient(properties, executor).createEntitlement(
                SmartcardSource.PHYSICAL,
                RmgEntitlementRequest.subscription(
                        "109687603246_TivuHD", "109687603246", "22",
                        Instant.parse("2026-09-24T08:44:52Z"),
                        Instant.parse("2030-09-24T08:44:52Z")
                )
        );
        server.verify();
    }
}
