package it.tivusat.cas.domain;

public record UaRange(
        long minUa,
        long maxUa,
        SmartcardFamily family,
        SmartcardRoutingTarget routingTarget
) {

    public UaRange {
        if (minUa > maxUa) {
            throw new IllegalArgumentException("UA range min cannot be greater than max");
        }
    }

    public boolean contains(long ua) {
        return ua >= minUa && ua <= maxUa;
    }

    public static UaRange of(
            long minUa,
            long maxUa,
            SmartcardFamily family,
            SmartcardRoutingTarget routingTarget
    ) {
        return new UaRange(minUa, maxUa, family, routingTarget);
    }
}