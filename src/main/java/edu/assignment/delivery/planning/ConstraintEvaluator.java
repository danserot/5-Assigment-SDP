package edu.assignment.delivery.planning;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.Route;
import edu.assignment.delivery.domain.RouteAssessment;
import edu.assignment.delivery.domain.RouteMetrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Shared hard constraints are applied before any strategy-specific scoring. */
public final class ConstraintEvaluator {
    private final DeliveryMetrics calculator = new DeliveryMetrics();

    public List<RouteAssessment> evaluate(List<Route> routes, DeliveryRequest request) {
        Objects.requireNonNull(request, "request");
        List<Route> snapshot = List.copyOf(Objects.requireNonNull(routes, "routes"));
        Set<String> ids = new HashSet<>();
        List<RouteAssessment> assessments = new ArrayList<>();
        for (Route route : snapshot) {
            if (!ids.add(route.id())) {
                throw new IllegalArgumentException("Duplicate route id: " + route.id());
            }
            RouteMetrics metrics = calculator.calculate(route, request);
            List<String> reasons = new ArrayList<>();
            if (!route.available()) {
                reasons.add("Route unavailable");
            }
            if (!route.origin().equals(request.origin())
                    || !route.destination().equals(request.destination())) {
                reasons.add("Origin or destination does not match the request");
            }
            if (metrics.totalCost() > request.maxBudget()) {
                reasons.add(String.format(Locale.ROOT, "Budget exceeded: %.2f > %.2f",
                        metrics.totalCost(), request.maxBudget()));
            }
            if (metrics.timeMinutes() > request.maxDeliveryMinutes()) {
                reasons.add(String.format(Locale.ROOT, "Deadline exceeded: %.0f > %d min",
                        metrics.timeMinutes(), request.maxDeliveryMinutes()));
            }
            if (metrics.emissionKg() > request.maxCarbonEmission()) {
                reasons.add(String.format(Locale.ROOT, "Emission limit exceeded: %.2f > %.2f kg CO2",
                        metrics.emissionKg(), request.maxCarbonEmission()));
            }
            assessments.add(new RouteAssessment(route, metrics, reasons, null));
        }
        return List.copyOf(assessments);
    }
}
