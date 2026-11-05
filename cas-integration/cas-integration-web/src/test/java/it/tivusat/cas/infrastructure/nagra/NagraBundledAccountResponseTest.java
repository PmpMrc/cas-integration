package it.tivusat.cas.infrastructure.nagra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.tivusat.cas.infrastructure.nagra.dto.NagraBundledAccountResponse;
import it.tivusat.cas.infrastructure.nagra.dto.NagraBundledDeviceResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NagraBundledAccountResponseTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldReadAccountAndDeviceStatesSeparately() throws Exception {
        String json = "{"
                + "\"_id\":\"109842219110_TivuHD\","
                + "\"billingAddress\":[{\"key\":\"postCode\",\"value\":\"0\"}],"
                + "\"status\":\"SUSPENDED\","
                + "\"networkId\":\"0\","
                + "\"suspensionMode\":\"MOP\","
                + "\"created\":\"2026-10-09T12:48:05Z\","
                + "\"modified\":\"2026-10-12T14:00:47Z\","
                + "\"devices\":[{"
                + "\"deviceId\":\"109842219110_TivuHD\","
                + "\"smartCardId\":\"1098422191\","
                + "\"caSN\":\"0\","
                + "\"device_status\":\"ENABLED\","
                + "\"device_created\":\"2026-10-01T00:00:00Z\","
                + "\"device_modified\":\"2026-10-01T00:00:00Z\","
                + "\"SCSetID\":\"236\""
                + "}]"
                + "}";

        NagraBundledAccountResponse account = objectMapper.readValue(
                json,
                NagraBundledAccountResponse.class
        );

        assertEquals("109842219110_TivuHD", account.getId());
        assertEquals("SUSPENDED", account.getStatus());
        assertEquals(
                Instant.parse("2026-10-09T12:48:05Z"),
                account.getCreatedAt()
        );
        assertEquals(
                Instant.parse("2026-10-12T14:00:47Z"),
                account.getUpdatedAt()
        );

        assertNotNull(account.getDevices());
        assertEquals(1, account.getDevices().size());

        NagraBundledDeviceResponse device = account.getDevices().get(0);

        assertEquals("109842219110_TivuHD", device.getDeviceId());
        assertEquals("1098422191", device.getSmartCardId());
        assertEquals("0", device.getCaSn());
        assertEquals("ENABLED", device.getStatus());
        assertEquals("236", device.getScSetId());
        assertEquals(
                Instant.parse("2026-10-01T00:00:00Z"),
                device.getCreatedAt()
        );
        assertEquals(
                Instant.parse("2026-10-01T00:00:00Z"),
                device.getUpdatedAt()
        );
    }

    @Test
    void shouldPreserveMissingFieldsForLaterValidation() throws Exception {
        NagraBundledAccountResponse account = objectMapper.readValue(
                "{\"_id\":\"109842219110_TivuHD\"}",
                NagraBundledAccountResponse.class
        );

        assertNull(account.getStatus());
        assertNull(account.getDevices());
        assertNull(account.getCreatedAt());
        assertNull(account.getUpdatedAt());
    }

    @Test
    void shouldReadEmptyDeviceList() throws Exception {
        NagraBundledAccountResponse account = objectMapper.readValue(
                "{\"_id\":\"109842219110_TivuHD\","
                        + "\"status\":\"CANCELLED\",\"devices\":[]}",
                NagraBundledAccountResponse.class
        );

        assertEquals("CANCELLED", account.getStatus());
        assertNotNull(account.getDevices());
        assertTrue(account.getDevices().isEmpty());
    }
}