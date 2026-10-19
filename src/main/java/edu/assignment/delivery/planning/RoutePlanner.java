package edu.assignment.delivery.planning;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.Route;
import edu.assignment.delivery.domain.RouteDecision;
import edu.assignment.delivery.strategy.RouteStrategy;

import java.util.List;
import java.util.Objects;

/** Strategy Context: stores an algorithm and delegates without choosing its implementation. */
public final class RoutePlanner {
    private RouteStrategy strategy;

    public RoutePlanner(RouteStrategy strategy) {
        setStrategy(strategy);
    }

    public void setStrategy(RouteStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public RouteDecision plan(List<Route> routes, DeliveryRequest request) {
        return strategy.selectRoute(routes, request);
    }
}
