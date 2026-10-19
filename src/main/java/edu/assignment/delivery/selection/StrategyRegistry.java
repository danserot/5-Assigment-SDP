package edu.assignment.delivery.selection;

import edu.assignment.delivery.strategy.RouteStrategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StrategyRegistry {
    private final Map<String, RouteStrategy> strategies = new LinkedHashMap<>();

    public void register(RouteStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy");
        String name = strategy.getStrategyName();
        edu.assignment.delivery.domain.Validation.text(name, "strategy name");
        if (strategies.putIfAbsent(name, strategy) != null) {
            throw new IllegalArgumentException("Duplicate strategy name: " + name);
        }
    }

    public List<RouteStrategy> all() {
        return List.copyOf(strategies.values());
    }

    public RouteStrategy get(String name) {
        RouteStrategy strategy = strategies.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown strategy: " + name);
        }
        return strategy;
    }
}
