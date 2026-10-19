package edu.assignment.delivery.demo;

import edu.assignment.delivery.domain.*;
import edu.assignment.delivery.selection.StrategyRegistry;
import edu.assignment.delivery.selection.StrategySelector;
import edu.assignment.delivery.strategy.*;

import java.util.List;
import java.util.Map;

public final class DemoData {
    public static final Location ORIGIN = new Location("Warehouse", 43.238, 76.945);
    public static final Location DESTINATION = new Location("Customer", 43.270, 76.990);

    private DemoData() {}

    public static List<Route> routes() {
        return List.of(
                route("R1-express", 10, 18, 2200, 80, 0, 5.0, 4, 0, true),
                route("R2-economy", 16, 38, 600, 40, 0, 6.0, 10, 0, true),
                route("R3-green", 12, 32, 1000, 45, 0, 0.6, 3, 0, true),
                route("R4-short", 7, 26, 1800, 70, 100, 2.8, 14, 4, true),
                route("R5-highway", 22, 20, 1700, 100, 400, 8.0, 2, 0, true),
                route("R6-balanced", 11, 25, 1400, 55, 0, 1.6, 4, 0, true),
                route("R7-closed", 9, 10, 200, 0, 0, 0.2, 0, 0, false),
                route("R8-rural", 19, 35, 900, 50, 0, 2.2, 3, 6, true));
    }

    private static Route route(String id, double distance, int time, double base,
                               double rate, double fee, double co2,
                               int traffic, int weather, boolean available) {
        return new Route(id, ORIGIN, DESTINATION, distance, time, base, rate,
                fee, co2, traffic, weather, available);
    }

    public static DeliveryRequest request(String id, DeliveryPriority priority,
                                          double budget, int deadline, boolean ecoPreferred,
                                          double maxEmission) {
        return new DeliveryRequest(id, ORIGIN, DESTINATION, priority,
                budget, deadline, ecoPreferred, maxEmission);
    }

    public static DeliveryRequest standardRequest() {
        return request("STANDARD-105", DeliveryPriority.STANDARD, 5000, 60, false, 10);
    }

    public static StrategyRegistry registry() {
        var registry = new StrategyRegistry();
        registry.register(new FastestRouteStrategy());
        registry.register(new CheapestRouteStrategy());
        registry.register(new EcoFriendlyRouteStrategy());
        registry.register(new BalancedRouteStrategy(0.40, 0.30, 0.10, 0.20));
        registry.register(new DistanceFirstStrategy());
        return registry;
    }

    public static StrategySelector selector(StrategyRegistry registry) {
        return new StrategySelector(Map.of(
                DeliveryPriority.URGENT, registry.get("Fastest"),
                DeliveryPriority.ECONOMY, registry.get("Cheapest"),
                DeliveryPriority.ECO, registry.get("Eco"),
                DeliveryPriority.STANDARD, registry.get("Balanced")), registry.get("Eco"));
    }
}
