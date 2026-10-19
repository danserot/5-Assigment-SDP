package edu.assignment.delivery.strategy;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.RouteAssessment;

import java.util.List;
import java.util.function.ToDoubleFunction;

public final class FastestRouteStrategy extends AbstractRouteStrategy {
    @Override
    public String getStrategyName() {
        return "Fastest";
    }

    @Override
    protected ToDoubleFunction<RouteAssessment> prepareScorer(
            List<RouteAssessment> eligible, DeliveryRequest request) {
        return a -> a.metrics().timeMinutes();
    }

    @Override
    protected String explain(RouteAssessment selected) {
        return String.format(java.util.Locale.ROOT,
                "Minimum effective time: %d base + %d traffic + %d weather = %.0f min",
                selected.route().estimatedMinutes(), selected.route().trafficPenaltyMinutes(),
                selected.route().weatherPenaltyMinutes(), selected.metrics().timeMinutes());
    }
}
