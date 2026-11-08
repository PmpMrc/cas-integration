package it.tivusat.cas.infrastructure.salesforce;

import java.net.URI;

public final class SalesforceUrlValidator {

    private SalesforceUrlValidator() {
    }

    public static URI validate(String value) {
        URI uri;

        try {
            uri = URI.create(value);
        } catch (RuntimeException exception) {
            throw invalidUrl();
        }

        String host = uri.getHost();

        boolean https = "https".equalsIgnoreCase(uri.getScheme());

        boolean localHttp =
                "http".equalsIgnoreCase(uri.getScheme())
                        && ("localhost".equalsIgnoreCase(host)
                        || "127.0.0.1".equals(host));

        if (host == null
                || (!https && !localHttp)
                || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw invalidUrl();
        }

        return uri;
    }

    private static IllegalArgumentException invalidUrl() {
        return new IllegalArgumentException(
                "Salesforce URL must use HTTPS, or HTTP on localhost "
                        + "for local tests, without credentials, query or fragment"
        );
    }
}