package edu.assignment.delivery.strategy;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.RouteAssessment;
import edu.assignment.delivery.domain.RouteMetrics;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

public final class BalancedRouteStrategy extends AbstractRouteStrategy {
    private static final double EPSILON = 1e-9;
    private final double timeWeight;
    private final double costWeight;
    private final double distanceWeight;
    private final double emissionWeight;

    public BalancedRouteStrategy(double timeWeight, double costWeight,
                                 double distanceWeight, double emissionWeight) {
        double[] weights = {timeWeight, costWeight, distanceWeight, emissionWeight};
        double sum = 0;
        for (double weight : weights) {
            if (!Double.isFinite(weight) || weight < 0 || weight > 1) {
                throw new IllegalArgumentException("Weights must be finite and between 0 and 1");
            }
            sum += weight;
        }
        if (Math.abs(sum - 1.0) > EPSILON) {
            throw new IllegalArgumentException("Weights must sum to 1.0");
        }
        this.timeWeight = timeWeight;
        this.costWeight = costWeight;
        this.distanceWeight = distanceWeight;
        this.emissionWeight = emissionWeight;
    }

    @Override
    public String getStrategyName() {
        return "Balanced";
    }

    @Override
    protected ToDoubleFunction<RouteAssessment> prepareScorer(
            List<RouteAssessment> eligible, DeliveryRequest request) {
        // Four linear scans, once per decision; scoring every route is then O(1).
        Range time = range(eligible, RouteMetrics::timeMinutes);
        Range cost = range(eligible, RouteMetrics::totalCost);
        Range distance = range(eligible, RouteMetrics::distanceKm);
        Range emission = range(eligible, RouteMetrics::emissionKg);
        return a -> time.normalize(a.metrics().timeMinutes()) * timeWeight
                + cost.normalize(a.metrics().totalCost()) * costWeight
                + distance.normalize(a.metrics().distanceKm()) * distanceWeight
                + emission.normalize(a.metrics().emissionKg()) * emissionWeight;
    }

    @Override
    protected String explain(RouteAssessment selected) {
        return String.format(Locale.ROOT,
                "Minimum normalized weighted score %.4f; weights time=%.2f, cost=%.2f, distance=%.2f, CO2=%.2f. "
                        + "Each criterion uses (value-min)/(max-min) among eligible routes; equal ranges contribute 0.",
                selected.score(), timeWeight, costWeight, distanceWeight, emissionWeight);
    }

    private static Range range(List<RouteAssessment> routes, ToDoubleFunction<RouteMetrics> metric) {
        var stats = routes.stream().map(RouteAssessment::metrics).mapToDouble(metric).summaryStatistics();
        return new Range(stats.getMin(), stats.getMax());
    }

    private record Range(double min, double max) {
        double normalize(double value) {
            return max == min ? 0 : (value - min) / (max - min);
        }
    }
}
