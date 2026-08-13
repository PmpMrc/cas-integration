package it.tivusat.cas.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmartcardValidatorTest {

    private final SmartcardValidator validator = new SmartcardValidator();

    @Test
    void shouldValidateCorrectSerialNumber() {
        assertDoesNotThrow(() -> validator.validateSerialNumber("109687603246"));
    }

    @Test
    void shouldExtractUaFromSerialNumber() {
        String ua = validator.extractUa("109687603246");

        assertEquals("1096876032", ua);
    }

    @Test
    void shouldRejectNullSerialNumber() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateSerialNumber(null)
        );

        assertTrue(exception.getMessage().contains("12 digits"));
    }

    @Test
    void shouldRejectSerialNumberWithInvalidFormat() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateSerialNumber("ABC")
        );

        assertTrue(exception.getMessage().contains("12 digits"));
    }

    @Test
    void shouldRejectSerialNumberWithLessThanTwelveDigits() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateSerialNumber("1096876032")
        );

        assertTrue(exception.getMessage().contains("12 digits"));
    }

    @Test
    void shouldRejectSerialNumberWithInvalidChecksum() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateSerialNumber("109687603200")
        );

        assertTrue(exception.getMessage().contains("checksum"));
    }
}