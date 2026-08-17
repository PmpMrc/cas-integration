package it.tivusat.cas.domain.exception;

import it.tivusat.cas.domain.SmartcardFamily;
import it.tivusat.cas.domain.SmartcardRoutingTarget;

public class UnsupportedSmartcardRangeException extends RuntimeException {

    private final String ua;
    private final SmartcardFamily family;
    private final SmartcardRoutingTarget routingTarget;

    public UnsupportedSmartcardRangeException(
            String ua,
            SmartcardFamily family,
            SmartcardRoutingTarget routingTarget
    ) {
        super(buildMessage(ua, family, routingTarget));
        this.ua = ua;
        this.family = family;
        this.routingTarget = routingTarget;
    }

    public String getUa() {
        return ua;
    }

    public SmartcardFamily getFamily() {
        return family;
    }

    public SmartcardRoutingTarget getRoutingTarget() {
        return routingTarget;
    }

    private static String buildMessage(
            String ua,
            SmartcardFamily family,
            SmartcardRoutingTarget routingTarget
    ) {
        if (routingTarget == SmartcardRoutingTarget.LEGACY_SOA_SMS) {
            return "Smartcard UA " + ua + " must be handled by legacy SOA/SMS";
        }

        return "Smartcard UA " + ua + " is not supported by configured Tivusat ranges";
    }
}