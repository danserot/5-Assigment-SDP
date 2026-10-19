package edu.assignment.delivery.strategy;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.RouteAssessment;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

/** Fifth strategy added after the original four-strategy application was compiled and run. */
public final class DistanceFirstStrategy extends AbstractRouteStrategy {
    @Override
    public String getStrategyName() {
        return "DistanceFirst";
    }

    @Override
    protected ToDoubleFunction<RouteAssessment> prepareScorer(
            List<RouteAssessment> eligible, DeliveryRequest request) {
        return a -> a.metrics().distanceKm();
    }

    @Override
    protected String explain(RouteAssessment selected) {
        return String.format(Locale.ROOT, "Minimum feasible distance: %.2f km",
                selected.metrics().distanceKm());
    }
}
