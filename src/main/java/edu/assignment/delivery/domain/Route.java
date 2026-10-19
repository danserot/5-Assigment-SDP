package edu.assignment.delivery.domain;

import java.util.Objects;

/** Emissions are estimated kg CO2; all costs use the same demonstration currency unit. */
public record Route(
        String id, Location origin, Location destination,
        double distanceKm, int estimatedMinutes, double baseCost,
        double costPerKm, double additionalFee, double carbonEmission,
        int trafficPenaltyMinutes, int weatherPenaltyMinutes, boolean available) {
    public Route {
        Validation.text(id, "id");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Validation.nonNegative(distanceKm, "distanceKm");
        Validation.nonNegative(estimatedMinutes, "estimatedMinutes");
        Validation.nonNegative(baseCost, "baseCost");
        Validation.nonNegative(costPerKm, "costPerKm");
        Validation.nonNegative(additionalFee, "additionalFee");
        Validation.nonNegative(carbonEmission, "carbonEmission");
        Validation.nonNegative(trafficPenaltyMinutes, "trafficPenaltyMinutes");
        Validation.nonNegative(weatherPenaltyMinutes, "weatherPenaltyMinutes");
    }
}
