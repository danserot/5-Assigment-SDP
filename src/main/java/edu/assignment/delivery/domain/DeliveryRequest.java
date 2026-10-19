package edu.assignment.delivery.domain;

import java.util.Objects;

public record DeliveryRequest(
        String id, Location origin, Location destination,
        DeliveryPriority priority, double maxBudget, int maxDeliveryMinutes,
        boolean ecoPreferred, double maxCarbonEmission) {
    public DeliveryRequest {
        Validation.text(id, "id");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(priority, "priority");
        Validation.nonNegative(maxBudget, "maxBudget");
        Validation.nonNegative(maxDeliveryMinutes, "maxDeliveryMinutes");
        Validation.nonNegative(maxCarbonEmission, "maxCarbonEmission");
    }
}
