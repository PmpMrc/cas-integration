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
            throw new IllegalArgumentException("Smartcard SN must be 12 digits");
        }

        String ua = sn.substring(0, 10);
        int actualChecksum = Integer.parseInt(sn.substring(10, 12));
        int expectedChecksum = calculateChecksum(Long.parseLong(ua));

        if (expectedChecksum != actualChecksum) {
            throw new IllegalArgumentException("Invalid smartcard checksum");
        }
    }

    public String extractUa(String sn) {
        validateSn(sn);
        return sn.substring(0, 10);
    }

    private int calculateChecksum(long ua) {
        return (int) (
                (
                        (
                                6 * (ua / 100000000L)
                                        + 19 * ((ua / 10000000L) % 10)
                                        + 8 * ((ua / 10000L) % 1000)
                                        + ((ua / 100L) % 100)
                        ) % 23
                                + (ua % 100)
                ) % 100
        );
    }

    public String buildSnFromUa(String ua) {
        if (ua == null || !ua.matches("\\d{10}")) {
            throw new IllegalArgumentException("Smartcard UA must contain exactly 10 digits");
        }

        int checksum = calculateChecksum(Long.parseLong(ua));
        return ua + String.format("%02d", checksum);
    }
}