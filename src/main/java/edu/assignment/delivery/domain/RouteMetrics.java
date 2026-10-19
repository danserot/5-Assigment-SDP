package edu.assignment.delivery.domain;

/** Effective values after request-specific surcharges and route penalties. */
public record RouteMetrics(double timeMinutes, double totalCost,
                           double distanceKm, double emissionKg) {
    public RouteMetrics {
        Validation.nonNegative(timeMinutes, "timeMinutes");
        Validation.nonNegative(totalCost, "totalCost");
        Validation.nonNegative(distanceKm, "distanceKm");
        Validation.nonNegative(emissionKg, "emissionKg");
    }
}
