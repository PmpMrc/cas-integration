package it.tivusat.cas.domain;

public class UaRange {

    private final long min;
    private final long max;
    private final SmartcardFamily family;
    private final SmartcardRoutingTarget routingTarget;

    public UaRange(long min, long max, SmartcardFamily family, SmartcardRoutingTarget routingTarget) {
        if (min > max) {
            throw new IllegalArgumentException("UA range min must be less than or equal to max");
        }
        this.min = min;
        this.max = max;
        this.family = family;
        this.routingTarget = routingTarget;
    }

    public static UaRange of(
            long min,
            long max,
            SmartcardFamily family,
            SmartcardRoutingTarget routingTarget
    ) {
        return new UaRange(min, max, family, routingTarget);
    }

    public boolean contains(long ua) {
        return ua >= min && ua <= max;
    }

    public long min() {
        return min;
    }

    public long max() {
        return max;
    }

    public SmartcardFamily family() {
        return family;
    }

    public SmartcardRoutingTarget routingTarget() {
        return routingTarget;
    }
}