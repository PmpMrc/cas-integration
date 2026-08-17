package it.tivusat.cas.domain;

import it.tivusat.cas.domain.exception.UnsupportedSmartcardRangeException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UaRangeClassifier {

    private static final List<UaRange> RANGES = List.of(
            // Merlin smartcards -> NEW REST INTERFACE
            UaRange.of(
                    1096876032L,
                    1098842111L,
                    SmartcardFamily.MERLIN,
                    SmartcardRoutingTarget.NEW_REST_INTERFACE
            ),
            UaRange.of(
                    1263140864L,
                    1263665151L,
                    SmartcardFamily.MERLIN,
                    SmartcardRoutingTarget.NEW_REST_INTERFACE
            ),

            // Virtual smartcards -> NEW REST INTERFACE
            UaRange.of(
                    1098842112L,
                    1100152831L,
                    SmartcardFamily.VIRTUAL,
                    SmartcardRoutingTarget.NEW_REST_INTERFACE
            ),

            /*
             * TODO verify with NAGRA/Tivusat.
             * The document row appears malformed:
             * min = 1260453888
             * max = 123009791
             *
             * Since max < min, this range is intentionally not enabled.
             */
            UaRange.of(
                    1263009792L,
                    1263140863L,
                    SmartcardFamily.VIRTUAL,
                    SmartcardRoutingTarget.NEW_REST_INTERFACE
            ),

            // Tiger smartcards -> legacy SOA/SMS
            UaRange.of(
                    1092026368L,
                    1094582271L,
                    SmartcardFamily.TIGER,
                    SmartcardRoutingTarget.LEGACY_SOA_SMS
            ),
            UaRange.of(
                    1094582272L,
                    1096679423L,
                    SmartcardFamily.TIGER,
                    SmartcardRoutingTarget.LEGACY_SOA_SMS
            ),
            UaRange.of(
                    1096679424L,
                    1096876031L,
                    SmartcardFamily.TIGER,
                    SmartcardRoutingTarget.LEGACY_SOA_SMS
            )
    );

    public UaRange classify(String ua) {
        if (ua == null || !ua.matches("\\d+")) {
            throw new IllegalArgumentException("UA must contain digits only");
        }

        long uaValue = Long.parseLong(ua);

        return RANGES.stream()
                .filter(range -> range.contains(uaValue))
                .findFirst()
                .orElseGet(() -> UaRange.of(
                        uaValue,
                        uaValue,
                        SmartcardFamily.UNKNOWN,
                        SmartcardRoutingTarget.UNSUPPORTED
                ));
    }

    public UaRange validateRestSupported(String ua) {
        UaRange range = classify(ua);

        if (range.routingTarget() != SmartcardRoutingTarget.NEW_REST_INTERFACE) {
            throw new UnsupportedSmartcardRangeException(
                    ua,
                    range.family(),
                    range.routingTarget()
            );
        }

        return range;
    }
}