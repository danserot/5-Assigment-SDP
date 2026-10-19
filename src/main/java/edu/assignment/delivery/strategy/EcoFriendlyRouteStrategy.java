package edu.assignment.delivery.strategy;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.RouteAssessment;

import java.util.List;
import java.util.function.ToDoubleFunction;

public final class EcoFriendlyRouteStrategy extends AbstractRouteStrategy {
    public static final double EMISSION_WEIGHT = 1.0;
    public static final double DISTANCE_WEIGHT = 0.05;

    @Override
    public String getStrategyName() {
        return "Eco";
    }

    @Override
    protected ToDoubleFunction<RouteAssessment> prepareScorer(
            List<RouteAssessment> eligible, DeliveryRequest request) {
        return a -> a.metrics().emissionKg() * EMISSION_WEIGHT
                + a.metrics().distanceKm() * DISTANCE_WEIGHT;
    }

    @Override
    protected String explain(RouteAssessment selected) {
        return String.format(java.util.Locale.ROOT,
                "Minimum eco score: %.2f kg CO2 * %.2f + %.2f km * %.2f = %.4f",
                selected.metrics().emissionKg(), EMISSION_WEIGHT, selected.metrics().distanceKm(),
                DISTANCE_WEIGHT, selected.score());
    }
}
