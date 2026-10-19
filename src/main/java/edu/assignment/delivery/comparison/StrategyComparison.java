package edu.assignment.delivery.comparison;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.Route;
import edu.assignment.delivery.planning.NoSuitableRouteException;
import edu.assignment.delivery.selection.StrategyRegistry;
import edu.assignment.delivery.strategy.RouteStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StrategyComparison {
    private final StrategyRegistry registry;

    public StrategyComparison(StrategyRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public List<ComparisonResult> compare(List<Route> routes, DeliveryRequest request) {
        List<Route> snapshot = List.copyOf(Objects.requireNonNull(routes, "routes"));
        Objects.requireNonNull(request, "request");
        List<ComparisonResult> results = new ArrayList<>();
        if (registry.all().isEmpty()) {
            throw new IllegalStateException("Register at least one strategy before comparison");
        }
        for (RouteStrategy strategy : registry.all()) {
            try {
                results.add(ComparisonResult.success(strategy.selectRoute(snapshot, request)));
            } catch (NoSuitableRouteException exception) {
                results.add(ComparisonResult.failure(strategy.getStrategyName(), exception.getMessage()));
            }
        }
        return List.copyOf(results);
    }
}
