package edu.assignment.delivery.strategy;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.Route;
import edu.assignment.delivery.domain.RouteDecision;

import java.util.List;

public interface RouteStrategy {
    RouteDecision selectRoute(List<Route> routes, DeliveryRequest request);

    String getStrategyName();
}
