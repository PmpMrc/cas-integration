package it.tivusat.cas.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmartcardValidatorTest {

    private final SmartcardValidator validator = new SmartcardValidator();

    @Test
    void shouldValidateValidSn() {
        validator.validateSn("109687603246");

        assertEquals("1096876032", validator.extractUa("109687603246"));
    }

    @Test
    void shouldBuildSnFromUa() {
        String sn = validator.buildSnFromUa("1096876032");

        assertEquals("109687603246", sn);
    }

    @Test
    void shouldFailWhenSnHasInvalidChecksum() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                validator.validateSn("109687603200");
            }
        });
    }

    @Test
    void shouldFailWhenSnIsNotTwelveDigits() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                validator.validateSn("123");
            }
        });
    }

    @Test
    void shouldFailWhenUaIsNotTenDigits() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                validator.buildSnFromUa("123");
            }
        });
    }
}