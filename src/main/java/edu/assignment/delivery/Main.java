package edu.assignment.delivery;

import edu.assignment.delivery.comparison.StrategyComparison;
import edu.assignment.delivery.demo.ConsoleReporter;
import edu.assignment.delivery.demo.DemoData;
import edu.assignment.delivery.domain.DeliveryPriority;
import edu.assignment.delivery.planning.NoSuitableRouteException;
import edu.assignment.delivery.planning.RoutePlanner;
import edu.assignment.delivery.strategy.BalancedRouteStrategy;
import edu.assignment.delivery.strategy.FastestRouteStrategy;

import java.util.List;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        var routes = DemoData.routes();
        var request = DemoData.standardRequest();
        var registry = DemoData.registry();

        section("1. Same request, all registered strategies");
        new StrategyComparison(registry).compare(routes, request).forEach(result -> {
            result.decision().ifPresent(d -> ConsoleReporter.decision(d, false));
            result.error().ifPresent(System.out::println);
        });

        section("2. Three runtime switches on the SAME RoutePlanner object");
        var planner = new RoutePlanner(registry.get("Fastest"));
        ConsoleReporter.decision(planner.plan(routes, request), false);
        for (String name : List.of("Cheapest", "Eco", "Balanced")) {
            planner.setStrategy(registry.get(name));
            ConsoleReporter.decision(planner.plan(routes, request), false);
        }

        section("3. Automatic policy selection");
        var selector = DemoData.selector(registry);
        var requests = List.of(
                DemoData.request("URGENT-1", DeliveryPriority.URGENT, 3500, 30, false, 10),
                DemoData.request("ECONOMY-2", DeliveryPriority.ECONOMY, 1500, 60, false, 10),
                DemoData.request("ECO-3", DeliveryPriority.ECO, 2000, 40, false, 1),
                request,
                DemoData.request("PREFERENCE-4", DeliveryPriority.STANDARD, 5000, 60, true, 10));
        for (var delivery : requests) {
            planner.setStrategy(selector.select(delivery));
            System.out.print(delivery.id() + ": ");
            ConsoleReporter.decision(planner.plan(routes, delivery), false);
        }

        section("4. Same Fastest algorithm, different budgets");
        planner.setStrategy(new FastestRouteStrategy());
        ConsoleReporter.decision(planner.plan(routes, request), false);
        var restricted = DemoData.request("BUDGET-2500", DeliveryPriority.STANDARD, 2500, 60, false, 10);
        ConsoleReporter.decision(planner.plan(routes, restricted), false);

        section("5. Decision explanation and every candidate");
        planner.setStrategy(registry.get("Balanced"));
        ConsoleReporter.decision(planner.plan(routes, request), true);

        section("6. Explicit edge and failure cases");
        expectFailure("Empty candidate list", () -> planner.plan(List.of(), request));
        expectFailure("No feasible route", () -> planner.plan(routes,
                DemoData.request("IMPOSSIBLE", DeliveryPriority.STANDARD, 100, 5, false, 0)));
        expectFailure("Invalid weights", () -> new BalancedRouteStrategy(0.5, 0.5, 0.5, 0.5));
        expectFailure("Negative route distance", () -> {
            var r = routes.get(0);
            new edu.assignment.delivery.domain.Route(r.id(), r.origin(), r.destination(),
                    -1, 20, 100, 10, 0, 1, 0, 0, true);
        });
    }

    private static void expectFailure(String label, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | NoSuitableRouteException exception) {
            System.out.println(label + ": handled " + exception.getClass().getSimpleName()
                    + " - " + exception.getMessage());
            return;
        }
        throw new IllegalStateException("Expected failure did not occur: " + label);
    }

    private static void section(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
