package it.tivusat.cas.domain;

import org.springframework.stereotype.Component;

@Component
public class SmartcardValidator {

    public void validateSerialNumber(String sn) {
        if (sn == null || !sn.matches("\\d{12}")) {
            throw new IllegalArgumentException("Smartcard SN must contain exactly 12 digits");
        }

        long fullSn = Long.parseLong(sn);
        long ua = fullSn / 100;
        int providedChecksum = (int) (fullSn % 100);
        int expectedChecksum = calculateChecksum(ua);

        if (providedChecksum != expectedChecksum) {
            throw new IllegalArgumentException("Invalid smartcard checksum");
        }
    }

    public String extractUa(String sn) {
        return sn.substring(0, 10);
    }

    private int calculateChecksum(long ua) {
        return (int) (
                (
                        6 * (ua / 100000000L)
                                + 19 * ((ua / 10000000L) % 10)
                                + 8 * ((ua / 10000L) % 1000)
                                + ((ua / 100L) % 100)
                ) % 23
                        + (ua % 100)
        ) % 100;
    }
}