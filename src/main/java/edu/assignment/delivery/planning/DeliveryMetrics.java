package edu.assignment.delivery.planning;

import edu.assignment.delivery.domain.DeliveryPriority;
import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.Route;
import edu.assignment.delivery.domain.RouteMetrics;

public final class DeliveryMetrics {
    public static final double URGENT_SURCHARGE = 300.0;

    public RouteMetrics calculate(Route route, DeliveryRequest request) {
        double time = (double) route.estimatedMinutes()
                + route.trafficPenaltyMinutes() + route.weatherPenaltyMinutes();
        double surcharge = request.priority() == DeliveryPriority.URGENT ? URGENT_SURCHARGE : 0;
        double cost = route.baseCost() + route.distanceKm() * route.costPerKm()
                + route.additionalFee() + surcharge;
        return new RouteMetrics(time, cost, route.distanceKm(), route.carbonEmission());
    }
}
