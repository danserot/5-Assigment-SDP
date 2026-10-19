package edu.assignment.delivery.strategy;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.RouteAssessment;

import java.util.List;
import java.util.function.ToDoubleFunction;

public final class CheapestRouteStrategy extends AbstractRouteStrategy {
    @Override
    public String getStrategyName() {
        return "Cheapest";
    }

    @Override
    protected ToDoubleFunction<RouteAssessment> prepareScorer(
            List<RouteAssessment> eligible, DeliveryRequest request) {
        return a -> a.metrics().totalCost();
    }

    @Override
    protected String explain(RouteAssessment selected) {
        double surcharge = selected.metrics().totalCost() - selected.route().baseCost()
                - selected.route().distanceKm() * selected.route().costPerKm()
                - selected.route().additionalFee();
        return String.format(java.util.Locale.ROOT,
                "Minimum total cost: %.2f base + %.2f km * %.2f/km + %.2f fee + %.2f priority = %.2f",
                selected.route().baseCost(), selected.route().distanceKm(),
                selected.route().costPerKm(), selected.route().additionalFee(),
                surcharge, selected.metrics().totalCost());
    }
}
