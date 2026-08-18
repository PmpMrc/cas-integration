package it.tivusat.cas.domain;

import org.springframework.stereotype.Component;

@Component
public class SmartcardValidator {

    private static final String SN_REGEX = "\\d{12}";

    public void validate(String sn) {
        validateSn(sn);
    }

    public void validateSn(String sn) {
        if (sn == null || !sn.matches(SN_REGEX)) {
            throw new IllegalArgumentException("Smartcard SN must contain exactly 12 digits");
        }

        long serialNumber = Long.parseLong(sn);
        int expectedChecksum = calculateChecksum(serialNumber);
        int actualChecksum = Integer.parseInt(sn.substring(10, 12));

        if (expectedChecksum != actualChecksum) {
            throw new IllegalArgumentException("Invalid smartcard checksum");
        }
    }

    public String extractUa(String sn) {
        validateSn(sn);
        return sn.substring(0, 10);
    }

    private int calculateChecksum(long serialNumber) {
        return (int) (
                (
                        6 * (serialNumber / 100000000L)
                                + 19 * (serialNumber / 10000000L % 10)
                                + 8 * (serialNumber / 10000L % 1000)
                                + (serialNumber / 100L % 100)
                ) % 23
                        + (serialNumber % 100)
        ) % 100;
    }
}