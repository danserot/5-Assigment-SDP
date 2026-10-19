# Полный код Assignment 5

Копируй каждый блок в файл по указанному пути. Порядок создания описан в STEP_BY_STEP_RU.md. Основной код, тесты и конфигурация совпадают с файлами проекта.

## pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>edu.assignment</groupId>
    <artifactId>smart-delivery</artifactId>
    <version>1.0.0</version>
    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.4.1</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>edu.assignment.delivery.Main</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## run.ps1

```powershell
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $deliveryJavac = 'javac.exe'
    $deliveryJava = 'java.exe'
    if ($env:JAVA_HOME) {
        $deliveryJavac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
        $deliveryJava = Join-Path $env:JAVA_HOME 'bin/java.exe'
    }
    New-Item -ItemType Directory -Path 'target/classes' -Force | Out-Null
    $deliverySources = @(Get-ChildItem -LiteralPath 'src/main/java' -Filter '*.java' -Recurse |
        ForEach-Object { $_.FullName })
    & $deliveryJavac --release 17 -encoding UTF-8 -d 'target/classes' @deliverySources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. Use JDK 17 or newer.' }
    & $deliveryJava -cp 'target/classes' edu.assignment.delivery.Main
    if ($LASTEXITCODE -ne 0) { throw 'Demo failed.' }
} finally {
    Pop-Location
}
```

## src/main/java/edu/assignment/delivery/comparison/ComparisonResult.java

```java
package edu.assignment.delivery.comparison;

import edu.assignment.delivery.domain.RouteDecision;

import java.util.Objects;
import java.util.Optional;

public record ComparisonResult(String strategyName, Optional<RouteDecision> decision,
                               Optional<String> error) {
    public ComparisonResult {
        Objects.requireNonNull(strategyName, "strategyName");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(error, "error");
        if (decision.isPresent() == error.isPresent()) {
            throw new IllegalArgumentException("Exactly one of decision or error must be present");
        }
    }

    public static ComparisonResult success(RouteDecision decision) {
        return new ComparisonResult(decision.strategyName(), Optional.of(decision), Optional.empty());
    }

    public static ComparisonResult failure(String name, String message) {
        return new ComparisonResult(name, Optional.empty(), Optional.of(message));
    }
}
```

## src/main/java/edu/assignment/delivery/comparison/StrategyComparison.java

```java
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
```

## src/main/java/edu/assignment/delivery/demo/ConsoleReporter.java

```java
package edu.assignment.delivery.demo;

import edu.assignment.delivery.domain.RouteAssessment;
import edu.assignment.delivery.domain.RouteDecision;

import java.util.Locale;

public final class ConsoleReporter {
    private ConsoleReporter() {}

    public static void decision(RouteDecision decision, boolean details) {
        System.out.printf(Locale.ROOT,
                "%-13s -> %-12s | time=%4.0f min | cost=%7.2f | distance=%4.1f km | CO2=%3.1f kg | score=%.4f%n",
                decision.strategyName(), decision.selectedRoute().id(),
                decision.metrics().timeMinutes(), decision.metrics().totalCost(),
                decision.metrics().distanceKm(), decision.metrics().emissionKg(), decision.score());
        if (details) {
            System.out.println("Reason: " + decision.explanation());
            for (RouteAssessment assessment : decision.assessments()) {
                if (assessment.eligible()) {
                    System.out.printf(Locale.ROOT, "  %s: eligible, objective=%.4f%n",
                            assessment.route().id(), assessment.score());
                } else {
                    System.out.println("  " + assessment.route().id() + " rejected: "
                            + String.join("; ", assessment.rejectionReasons()));
                }
            }
        }
    }
}
```

## src/main/java/edu/assignment/delivery/demo/DemoData.java

```java
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
```

## src/main/java/edu/assignment/delivery/domain/DeliveryPriority.java

```java
package edu.assignment.delivery.domain;

public enum DeliveryPriority {
    URGENT, ECONOMY, ECO, STANDARD
}
```

## src/main/java/edu/assignment/delivery/domain/DeliveryRequest.java

```java
package edu.assignment.delivery.domain;

import java.util.Objects;

public record DeliveryRequest(
        String id, Location origin, Location destination,
        DeliveryPriority priority, double maxBudget, int maxDeliveryMinutes,
        boolean ecoPreferred, double maxCarbonEmission) {
    public DeliveryRequest {
        Validation.text(id, "id");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(priority, "priority");
        Validation.nonNegative(maxBudget, "maxBudget");
        Validation.nonNegative(maxDeliveryMinutes, "maxDeliveryMinutes");
        Validation.nonNegative(maxCarbonEmission, "maxCarbonEmission");
    }
}
```

## src/main/java/edu/assignment/delivery/domain/Location.java

```java
package edu.assignment.delivery.domain;

public record Location(String name, double latitude, double longitude) {
    public Location {
        Validation.text(name, "name");
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid geographic coordinates");
        }
    }
}
```

## src/main/java/edu/assignment/delivery/domain/Route.java

```java
package edu.assignment.delivery.domain;

import java.util.Objects;

/** Emissions are estimated kg CO2; all costs use the same demonstration currency unit. */
public record Route(
        String id, Location origin, Location destination,
        double distanceKm, int estimatedMinutes, double baseCost,
        double costPerKm, double additionalFee, double carbonEmission,
        int trafficPenaltyMinutes, int weatherPenaltyMinutes, boolean available) {
    public Route {
        Validation.text(id, "id");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Validation.nonNegative(distanceKm, "distanceKm");
        Validation.nonNegative(estimatedMinutes, "estimatedMinutes");
        Validation.nonNegative(baseCost, "baseCost");
        Validation.nonNegative(costPerKm, "costPerKm");
        Validation.nonNegative(additionalFee, "additionalFee");
        Validation.nonNegative(carbonEmission, "carbonEmission");
        Validation.nonNegative(trafficPenaltyMinutes, "trafficPenaltyMinutes");
        Validation.nonNegative(weatherPenaltyMinutes, "weatherPenaltyMinutes");
    }
}
```

## src/main/java/edu/assignment/delivery/domain/RouteAssessment.java

```java
package edu.assignment.delivery.domain;

import java.util.List;
import java.util.Objects;

/** score is absent (null) for rejected routes or before ranking. */
public record RouteAssessment(Route route, RouteMetrics metrics,
                              List<String> rejectionReasons, Double score) {
    public RouteAssessment {
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(metrics, "metrics");
        rejectionReasons = List.copyOf(rejectionReasons);
        if (score != null && (!Double.isFinite(score) || score < 0)) {
            throw new IllegalArgumentException("score must be finite and non-negative");
        }
        if (!rejectionReasons.isEmpty() && score != null) {
            throw new IllegalArgumentException("Rejected routes must not be ranked");
        }
    }

    public boolean eligible() {
        return rejectionReasons.isEmpty();
    }

    public RouteAssessment withScore(double score) {
        return new RouteAssessment(route, metrics, rejectionReasons, score);
    }
}
```

## src/main/java/edu/assignment/delivery/domain/RouteDecision.java

```java
package edu.assignment.delivery.domain;

import java.util.List;
import java.util.Objects;

public record RouteDecision(String strategyName, Route selectedRoute,
                            RouteMetrics metrics, double score, String explanation,
                            List<RouteAssessment> assessments) {
    public RouteDecision {
        Validation.text(strategyName, "strategyName");
        Objects.requireNonNull(selectedRoute, "selectedRoute");
        Objects.requireNonNull(metrics, "metrics");
        Validation.nonNegative(score, "score");
        Validation.text(explanation, "explanation");
        assessments = List.copyOf(assessments);
    }
}
```

## src/main/java/edu/assignment/delivery/domain/RouteMetrics.java

```java
package edu.assignment.delivery.domain;

/** Effective values after request-specific surcharges and route penalties. */
public record RouteMetrics(double timeMinutes, double totalCost,
                           double distanceKm, double emissionKg) {
    public RouteMetrics {
        Validation.nonNegative(timeMinutes, "timeMinutes");
        Validation.nonNegative(totalCost, "totalCost");
        Validation.nonNegative(distanceKm, "distanceKm");
        Validation.nonNegative(emissionKg, "emissionKg");
    }
}
```

## src/main/java/edu/assignment/delivery/domain/Validation.java

```java
package edu.assignment.delivery.domain;

public final class Validation {
    private Validation() {}

    public static String text(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static double nonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
        return value;
    }

    public static int nonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
        return value;
    }
}
```

## src/main/java/edu/assignment/delivery/Main.java

```java
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
```

## src/main/java/edu/assignment/delivery/planning/ConstraintEvaluator.java

```java
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
```

## src/main/java/edu/assignment/delivery/planning/DeliveryMetrics.java

```java
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
```

## src/main/java/edu/assignment/delivery/planning/NoSuitableRouteException.java

```java
package edu.assignment.delivery.planning;

import edu.assignment.delivery.domain.RouteAssessment;

import java.util.List;

public final class NoSuitableRouteException extends RuntimeException {
    private final List<RouteAssessment> assessments;

    public NoSuitableRouteException(String message, List<RouteAssessment> assessments) {
        super(message);
        this.assessments = List.copyOf(assessments);
    }

    public List<RouteAssessment> assessments() {
        return assessments;
    }
}
```

## src/main/java/edu/assignment/delivery/planning/RoutePlanner.java

```java
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
```

## src/main/java/edu/assignment/delivery/selection/StrategyRegistry.java

```java
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
```

## src/main/java/edu/assignment/delivery/selection/StrategySelector.java

```java
package edu.assignment.delivery.selection;

import edu.assignment.delivery.domain.DeliveryPriority;
import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.strategy.RouteStrategy;

import java.util.Map;
import java.util.Objects;

/** Selects a policy; route scoring remains entirely in the selected Strategy. */
public final class StrategySelector {
    private final Map<DeliveryPriority, RouteStrategy> byPriority;
    private final RouteStrategy ecoPreferredStrategy;

    public StrategySelector(Map<DeliveryPriority, RouteStrategy> byPriority,
                            RouteStrategy ecoPreferredStrategy) {
        this.byPriority = Map.copyOf(byPriority);
        this.ecoPreferredStrategy = Objects.requireNonNull(ecoPreferredStrategy, "ecoPreferredStrategy");
    }

    public RouteStrategy select(DeliveryRequest request) {
        Objects.requireNonNull(request, "request");
        // Explicit priorities win; the preference refines only STANDARD deliveries.
        if (request.priority() == DeliveryPriority.STANDARD && request.ecoPreferred()) {
            return ecoPreferredStrategy;
        }
        RouteStrategy selected = byPriority.get(request.priority());
        if (selected == null) {
            throw new IllegalArgumentException("No strategy registered for " + request.priority());
        }
        return selected;
    }
}
```

## src/main/java/edu/assignment/delivery/strategy/AbstractRouteStrategy.java

```java
package edu.assignment.delivery.strategy;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.Route;
import edu.assignment.delivery.domain.RouteAssessment;
import edu.assignment.delivery.domain.RouteDecision;
import edu.assignment.delivery.planning.ConstraintEvaluator;
import edu.assignment.delivery.planning.NoSuitableRouteException;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Common filter/rank workflow; each strategy supplies its own objective function. */
public abstract class AbstractRouteStrategy implements RouteStrategy {
    private final ConstraintEvaluator constraints = new ConstraintEvaluator();

    @Override
    public final RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        List<RouteAssessment> assessed = constraints.evaluate(routes, request);
        List<RouteAssessment> eligible = assessed.stream().filter(RouteAssessment::eligible).toList();
        if (eligible.isEmpty()) {
            throw new NoSuitableRouteException(
                    "No suitable route for " + request.id() + " using " + getStrategyName(), assessed);
        }
        ToDoubleFunction<RouteAssessment> scorer = prepareScorer(eligible, request);
        List<RouteAssessment> ranked = assessed.stream()
                .map(a -> a.eligible() ? a.withScore(scorer.applyAsDouble(a)) : a).toList();
        RouteAssessment best = ranked.stream().filter(RouteAssessment::eligible)
                .min(Comparator.comparingDouble((RouteAssessment a) -> a.score())
                        .thenComparing(a -> a.route().id())).orElseThrow();
        return new RouteDecision(getStrategyName(), best.route(), best.metrics(), best.score(),
                explain(best), ranked);
    }

    protected abstract ToDoubleFunction<RouteAssessment> prepareScorer(
            List<RouteAssessment> eligible, DeliveryRequest request);

    protected abstract String explain(RouteAssessment selected);
}
```

## src/main/java/edu/assignment/delivery/strategy/BalancedRouteStrategy.java

```java
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
```

## src/main/java/edu/assignment/delivery/strategy/CheapestRouteStrategy.java

```java
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
```

## src/main/java/edu/assignment/delivery/strategy/DistanceFirstStrategy.java

```java
package edu.assignment.delivery.strategy;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.RouteAssessment;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

/** Fifth strategy added after the original four-strategy application was compiled and run. */
public final class DistanceFirstStrategy extends AbstractRouteStrategy {
    @Override
    public String getStrategyName() {
        return "DistanceFirst";
    }

    @Override
    protected ToDoubleFunction<RouteAssessment> prepareScorer(
            List<RouteAssessment> eligible, DeliveryRequest request) {
        return a -> a.metrics().distanceKm();
    }

    @Override
    protected String explain(RouteAssessment selected) {
        return String.format(Locale.ROOT, "Minimum feasible distance: %.2f km",
                selected.metrics().distanceKm());
    }
}
```

## src/main/java/edu/assignment/delivery/strategy/EcoFriendlyRouteStrategy.java

```java
package edu.assignment.delivery.strategy;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.RouteAssessment;

import java.util.List;
import java.util.function.ToDoubleFunction;

public final class EcoFriendlyRouteStrategy extends AbstractRouteStrategy {
    public static final double EMISSION_WEIGHT = 1.0;
    public static final double DISTANCE_WEIGHT = 0.05;

    @Override
    public String getStrategyName() {
        return "Eco";
    }

    @Override
    protected ToDoubleFunction<RouteAssessment> prepareScorer(
            List<RouteAssessment> eligible, DeliveryRequest request) {
        return a -> a.metrics().emissionKg() * EMISSION_WEIGHT
                + a.metrics().distanceKm() * DISTANCE_WEIGHT;
    }

    @Override
    protected String explain(RouteAssessment selected) {
        return String.format(java.util.Locale.ROOT,
                "Minimum eco score: %.2f kg CO2 * %.2f + %.2f km * %.2f = %.4f",
                selected.metrics().emissionKg(), EMISSION_WEIGHT, selected.metrics().distanceKm(),
                DISTANCE_WEIGHT, selected.score());
    }
}
```

## src/main/java/edu/assignment/delivery/strategy/FastestRouteStrategy.java

```java
package edu.assignment.delivery.strategy;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.RouteAssessment;

import java.util.List;
import java.util.function.ToDoubleFunction;

public final class FastestRouteStrategy extends AbstractRouteStrategy {
    @Override
    public String getStrategyName() {
        return "Fastest";
    }

    @Override
    protected ToDoubleFunction<RouteAssessment> prepareScorer(
            List<RouteAssessment> eligible, DeliveryRequest request) {
        return a -> a.metrics().timeMinutes();
    }

    @Override
    protected String explain(RouteAssessment selected) {
        return String.format(java.util.Locale.ROOT,
                "Minimum effective time: %d base + %d traffic + %d weather = %.0f min",
                selected.route().estimatedMinutes(), selected.route().trafficPenaltyMinutes(),
                selected.route().weatherPenaltyMinutes(), selected.metrics().timeMinutes());
    }
}
```

## src/main/java/edu/assignment/delivery/strategy/RouteStrategy.java

```java
package edu.assignment.delivery.strategy;

import edu.assignment.delivery.domain.DeliveryRequest;
import edu.assignment.delivery.domain.Route;
import edu.assignment.delivery.domain.RouteDecision;

import java.util.List;

public interface RouteStrategy {
    RouteDecision selectRoute(List<Route> routes, DeliveryRequest request);

    String getStrategyName();
}
```

## src/test/java/edu/assignment/delivery/ConstraintTest.java

```java
package edu.assignment.delivery;

import edu.assignment.delivery.demo.DemoData;
import edu.assignment.delivery.domain.*;
import edu.assignment.delivery.planning.ConstraintEvaluator;
import edu.assignment.delivery.planning.DeliveryMetrics;
import edu.assignment.delivery.planning.NoSuitableRouteException;
import edu.assignment.delivery.strategy.FastestRouteStrategy;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintTest {
    @Test
    void unavailableRouteCannotWinDespiteBetterTime() {
        var unavailable = TestData.route("A", 1, 1, 1, 0, 0, 0, 0, 0, false);
        var valid = TestData.simple("B", 5, 20, 100, 1);
        var decision = new FastestRouteStrategy().selectRoute(
                List.of(unavailable, valid), TestData.request(1000, 60, 10));
        assertEquals("B", decision.selectedRoute().id());
        assertEquals(List.of("Route unavailable"), decision.assessments().get(0).rejectionReasons());
    }

    @Test
    void budgetIsAHardConstraintBeforeScoring() {
        var routes = List.of(TestData.simple("A", 1, 1, 1001, 1),
                TestData.simple("B", 5, 20, 1000, 1));
        var result = new FastestRouteStrategy().selectRoute(routes, TestData.request(1000, 60, 10));
        assertEquals("B", result.selectedRoute().id());
        assertTrue(result.assessments().get(0).rejectionReasons().get(0).contains("Budget exceeded"));
    }

    @Test
    void deadlineUsesFinalTimeIncludingBothPenalties() {
        var slowAfterPenalties = TestData.route("A", 1, 5, 10, 0, 0, 1, 10, 10, true);
        var valid = TestData.simple("B", 5, 20, 100, 1);
        var result = new FastestRouteStrategy().selectRoute(
                List.of(slowAfterPenalties, valid), TestData.request(1000, 20, 10));
        assertEquals("B", result.selectedRoute().id());
        assertTrue(result.assessments().get(0).rejectionReasons().get(0).contains("Deadline exceeded"));
    }

    @Test
    void emissionLimitExcludesOtherwiseFastestRoute() {
        var routes = List.of(TestData.simple("A", 1, 1, 10, 5),
                TestData.simple("B", 5, 20, 100, 1));
        assertEquals("B", new FastestRouteStrategy().selectRoute(routes,
                TestData.request(1000, 60, 1)).selectedRoute().id());
    }

    @Test
    void mismatchedDestinationIsRejected() {
        var wrongDestination = new Route("A", DemoData.ORIGIN,
                new Location("Other", 0, 0), 1, 1, 1, 0, 0, 0, 0, 0, true);
        var assessed = new ConstraintEvaluator().evaluate(
                List.of(wrongDestination), TestData.request(1000, 60, 10));
        assertFalse(assessed.get(0).eligible());
        assertTrue(assessed.get(0).rejectionReasons().get(0).contains("destination"));
    }

    @Test
    void exactBudgetDeadlineAndEmissionLimitsAreInclusive() {
        var route = TestData.simple("A", 5, 30, 100, 2);
        var result = new FastestRouteStrategy().selectRoute(
                List.of(route), TestData.request(100, 30, 2));
        assertEquals("A", result.selectedRoute().id());
    }

    @Test
    void zeroCostAndZeroEmissionCanSatisfyZeroLimits() {
        var route = TestData.simple("A", 0, 0, 0, 0);
        assertEquals("A", new FastestRouteStrategy().selectRoute(
                List.of(route), TestData.request(0, 0, 0)).selectedRoute().id());
    }

    @Test
    void urgentSurchargeIsIncludedInBudgetFiltering() {
        var request = DemoData.request("U", DeliveryPriority.URGENT, 1000, 60, false, 10);
        var route = TestData.simple("A", 5, 10, 800, 1);
        assertEquals(1100, new DeliveryMetrics().calculate(route, request).totalCost());
        assertThrows(NoSuitableRouteException.class,
                () -> new FastestRouteStrategy().selectRoute(List.of(route), request));
    }

    @Test
    void noValidRouteThrowsExceptionWithEveryRejectionReason() {
        var route = TestData.route("A", 1, 100, 1000, 0, 0, 5, 0, 0, false);
        var error = assertThrows(NoSuitableRouteException.class,
                () -> new FastestRouteStrategy().selectRoute(
                        List.of(route), TestData.request(10, 10, 1)));
        assertEquals(1, error.assessments().size());
        assertEquals(4, error.assessments().get(0).rejectionReasons().size());
    }

    @Test
    void emptyListIsHandledExplicitly() {
        var error = assertThrows(NoSuitableRouteException.class,
                () -> new FastestRouteStrategy().selectRoute(List.of(), DemoData.standardRequest()));
        assertTrue(error.assessments().isEmpty());
    }

    @Test
    void nullListNullRequestAndNullCandidateAreRejected() {
        var strategy = new FastestRouteStrategy();
        assertThrows(NullPointerException.class,
                () -> strategy.selectRoute(null, DemoData.standardRequest()));
        assertThrows(NullPointerException.class,
                () -> strategy.selectRoute(DemoData.routes(), null));
        assertThrows(NullPointerException.class,
                () -> strategy.selectRoute(Arrays.asList((Route) null), DemoData.standardRequest()));
    }

    @Test
    void duplicateIdsAreRejectedRatherThanProducingAmbiguousExplanations() {
        Route route = TestData.simple("A", 5, 10, 100, 1);
        assertThrows(IllegalArgumentException.class,
                () -> new FastestRouteStrategy().selectRoute(
                        List.of(route, route), DemoData.standardRequest()));
    }

    @Test
    void negativeDistanceAndCostAndNonFiniteDataAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> TestData.simple("A", -1, 10, 100, 1));
        assertThrows(IllegalArgumentException.class, () -> TestData.simple("A", 1, 10, -100, 1));
        assertThrows(IllegalArgumentException.class, () -> TestData.simple("A", Double.NaN, 10, 100, 1));
        assertThrows(IllegalArgumentException.class,
                () -> TestData.request(Double.POSITIVE_INFINITY, 60, 10));
    }

    @Test
    void missingPriorityAndInvalidCoordinatesAreRejected() {
        assertThrows(NullPointerException.class, () -> new DeliveryRequest("A",
                DemoData.ORIGIN, DemoData.DESTINATION, null, 100, 30, false, 10));
        assertThrows(IllegalArgumentException.class, () -> new Location("Invalid", 91, 0));
    }
}
```

## src/test/java/edu/assignment/delivery/PlanningTest.java

```java
package edu.assignment.delivery;

import edu.assignment.delivery.comparison.StrategyComparison;
import edu.assignment.delivery.demo.DemoData;
import edu.assignment.delivery.domain.DeliveryPriority;
import edu.assignment.delivery.planning.RoutePlanner;
import edu.assignment.delivery.selection.StrategyRegistry;
import edu.assignment.delivery.selection.StrategySelector;
import edu.assignment.delivery.strategy.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanningTest {
    @Test
    void sameContextPerformsThreeRuntimeSwitches() {
        var planner = new RoutePlanner(new FastestRouteStrategy());
        var routes = DemoData.routes();
        var request = DemoData.standardRequest();
        assertEquals("R1-express", planner.plan(routes, request).selectedRoute().id());
        planner.setStrategy(new CheapestRouteStrategy());
        assertEquals("R2-economy", planner.plan(routes, request).selectedRoute().id());
        planner.setStrategy(new EcoFriendlyRouteStrategy());
        assertEquals("R3-green", planner.plan(routes, request).selectedRoute().id());
        planner.setStrategy(new BalancedRouteStrategy(.4, .3, .1, .2));
        assertEquals("R6-balanced", planner.plan(routes, request).selectedRoute().id());
    }

    @ParameterizedTest
    @CsvSource({"URGENT,Fastest", "ECONOMY,Cheapest", "ECO,Eco", "STANDARD,Balanced"})
    void selectorMapsEachExplicitPriority(DeliveryPriority priority, String expectedName) {
        var selector = DemoData.selector(DemoData.registry());
        var request = DemoData.request("A", priority, 5000, 60, false, 10);
        assertEquals(expectedName, selector.select(request).getStrategyName());
    }

    @Test
    void ecoPreferenceRefinesStandardButDoesNotOverrideUrgency() {
        var selector = DemoData.selector(DemoData.registry());
        assertEquals("Eco", selector.select(DemoData.request(
                "A", DeliveryPriority.STANDARD, 5000, 60, true, 10)).getStrategyName());
        assertEquals("Fastest", selector.select(DemoData.request(
                "B", DeliveryPriority.URGENT, 5000, 60, true, 10)).getStrategyName());
    }

    @Test
    void selectorMappingCanBeReplacedWithoutChangingContext() {
        var selector = new StrategySelector(
                Map.of(DeliveryPriority.STANDARD, new DistanceFirstStrategy()),
                new EcoFriendlyRouteStrategy());
        var planner = new RoutePlanner(selector.select(DemoData.standardRequest()));
        assertEquals("R4-short", planner.plan(DemoData.routes(), DemoData.standardRequest())
                .selectedRoute().id());
    }

    @Test
    void comparisonExecutesAllFiveAlgorithmsForTheSameRequest() {
        var registry = DemoData.registry();
        var results = new StrategyComparison(registry)
                .compare(DemoData.routes(), DemoData.standardRequest());
        assertEquals(5, results.size());
        assertTrue(results.stream().allMatch(r -> r.decision().isPresent() && r.error().isEmpty()));
        assertEquals("R1-express", results.get(0).decision().orElseThrow().selectedRoute().id());
        assertEquals("R2-economy", results.get(1).decision().orElseThrow().selectedRoute().id());
        assertEquals("R4-short", results.get(4).decision().orElseThrow().selectedRoute().id());
    }

    @Test
    void comparisonRecordsEachFailureInsteadOfReturningNullOrStoppingEarly() {
        var results = new StrategyComparison(DemoData.registry()).compare(
                DemoData.routes(), TestData.request(0, 0, 0));
        assertEquals(5, results.size());
        assertTrue(results.stream().allMatch(r -> r.decision().isEmpty() && r.error().isPresent()));
    }

    @Test
    void contextRejectsNullStrategyOnConstructionAndSwitch() {
        assertThrows(NullPointerException.class, () -> new RoutePlanner(null));
        var planner = new RoutePlanner(new FastestRouteStrategy());
        assertThrows(NullPointerException.class, () -> planner.setStrategy(null));
        assertEquals("R1-express", planner.plan(DemoData.routes(), DemoData.standardRequest())
                .selectedRoute().id());
    }

    @Test
    void registryRejectsDuplicateAndUnknownNames() {
        var registry = new StrategyRegistry();
        registry.register(new FastestRouteStrategy());
        assertThrows(IllegalArgumentException.class, () -> registry.register(new FastestRouteStrategy()));
        assertThrows(IllegalArgumentException.class, () -> registry.get("Missing"));
        assertThrows(UnsupportedOperationException.class, () -> registry.all().clear());
    }

    @Test
    void unmappedPriorityAndEmptyComparisonRegistryHaveExplicitErrors() {
        var selector = new StrategySelector(Map.of(), new EcoFriendlyRouteStrategy());
        assertThrows(IllegalArgumentException.class, () -> selector.select(DemoData.standardRequest()));
        assertThrows(IllegalStateException.class, () -> new StrategyComparison(new StrategyRegistry())
                .compare(List.of(), DemoData.standardRequest()));
    }
}
```

## src/test/java/edu/assignment/delivery/StrategyTest.java

```java
package edu.assignment.delivery;

import edu.assignment.delivery.demo.DemoData;
import edu.assignment.delivery.domain.Route;
import edu.assignment.delivery.strategy.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StrategyTest {
    static Stream<Arguments> demonstrationWinners() {
        return Stream.of(
                Arguments.of(new FastestRouteStrategy(), "R1-express"),
                Arguments.of(new CheapestRouteStrategy(), "R2-economy"),
                Arguments.of(new EcoFriendlyRouteStrategy(), "R3-green"),
                Arguments.of(new BalancedRouteStrategy(.4, .3, .1, .2), "R6-balanced"),
                Arguments.of(new DistanceFirstStrategy(), "R4-short"));
    }

    @ParameterizedTest
    @MethodSource("demonstrationWinners")
    void strategiesChooseTheirExpectedRoutes(RouteStrategy strategy, String expected) {
        var result = strategy.selectRoute(DemoData.routes(), DemoData.standardRequest());
        assertEquals(expected, result.selectedRoute().id());
        assertEquals(8, result.assessments().size());
        assertFalse(result.explanation().isBlank());
    }

    @Test
    void fastestUsesTrafficAndWeatherInsteadOfBaseTimeAlone() {
        var apparentlyFast = TestData.route("A", 5, 10, 100, 0, 0, 1, 15, 10, true);
        var actuallyFast = TestData.simple("B", 6, 20, 200, 1);
        var decision = new FastestRouteStrategy().selectRoute(
                List.of(apparentlyFast, actuallyFast), TestData.request(1000, 60, 10));
        assertEquals("B", decision.selectedRoute().id());
        assertEquals(20, decision.metrics().timeMinutes());
    }

    @Test
    void cheapestUsesFullCostRatherThanBaseCost() {
        var lowBase = TestData.route("A", 20, 20, 10, 100, 100, 1, 0, 0, true);
        var lowTotal = TestData.route("B", 5, 20, 100, 10, 20, 1, 0, 0, true);
        var decision = new CheapestRouteStrategy().selectRoute(
                List.of(lowBase, lowTotal), TestData.request(5000, 60, 10));
        assertEquals("B", decision.selectedRoute().id());
        assertEquals(170, decision.metrics().totalCost());
        assertTrue(decision.explanation().contains("170.00"));
    }

    @Test
    void ecoIncludesDistanceAsWellAsEmissions() {
        var lowEmissionLongTrip = TestData.simple("A", 100, 20, 100, 1);
        var shortTrip = TestData.simple("B", 1, 20, 100, 2);
        var decision = new EcoFriendlyRouteStrategy().selectRoute(
                List.of(lowEmissionLongTrip, shortTrip), TestData.request(1000, 60, 10));
        assertEquals("B", decision.selectedRoute().id());
        assertEquals(2.05, decision.score(), 1e-9);
    }

    @Test
    void balancedUsesNormalizedMetricsRatherThanRawMoney() {
        var fastExpensive = TestData.simple("A", 10, 10, 10000, 1);
        var slowCheap = TestData.simple("B", 10, 20, 100, 1);
        var decision = new BalancedRouteStrategy(.8, .2, 0, 0).selectRoute(
                List.of(fastExpensive, slowCheap), TestData.request(20000, 60, 10));
        assertEquals("A", decision.selectedRoute().id());
        assertEquals(.2, decision.score(), 1e-9);
    }

    @Test
    void changingBalancedWeightsChangesWinner() {
        var routes = List.of(TestData.simple("A", 10, 10, 1000, 1),
                TestData.simple("B", 10, 20, 100, 1));
        var request = TestData.request(2000, 60, 10);
        assertEquals("A", new BalancedRouteStrategy(.9, .1, 0, 0)
                .selectRoute(routes, request).selectedRoute().id());
        assertEquals("B", new BalancedRouteStrategy(.1, .9, 0, 0)
                .selectRoute(routes, request).selectedRoute().id());
    }

    @Test
    void equalRangesProduceZeroInsteadOfNaNAndTieBreakById() {
        var routes = List.of(TestData.simple("B", 1, 10, 100, 1),
                TestData.simple("A", 1, 10, 100, 1));
        var result = new BalancedRouteStrategy(.4, .3, .1, .2)
                .selectRoute(routes, TestData.request(1000, 60, 10));
        assertEquals("A", result.selectedRoute().id());
        assertEquals(0, result.score());
    }

    @Test
    void rejectedOutlierDoesNotDistortBalancedNormalization() {
        var routes = List.of(TestData.simple("A", 10, 10, 1000, 1),
                TestData.simple("B", 10, 20, 100, 1),
                TestData.simple("REJECTED", 10, 10000, 1000000, 1));
        var decision = new BalancedRouteStrategy(.6, .4, 0, 0)
                .selectRoute(routes, TestData.request(2000, 60, 10));
        assertEquals("A", decision.selectedRoute().id());
        assertEquals(.4, decision.score(), 1e-9);
        assertNull(decision.assessments().get(2).score());
    }

    @Test
    void sameAlgorithmRespondsToChangedRequestBudget() {
        var fastest = new FastestRouteStrategy();
        assertEquals("R1-express", fastest.selectRoute(DemoData.routes(),
                TestData.request(5000, 60, 10)).selectedRoute().id());
        assertEquals("R6-balanced", fastest.selectRoute(DemoData.routes(),
                TestData.request(2500, 60, 10)).selectedRoute().id());
    }

    @Test
    void doesNotMutateCandidateListAndReturnsImmutableAssessments() {
        var routes = new ArrayList<>(DemoData.routes());
        var before = List.copyOf(routes);
        var result = new FastestRouteStrategy().selectRoute(routes, DemoData.standardRequest());
        assertEquals(before, routes);
        assertThrows(UnsupportedOperationException.class, () -> result.assessments().clear());
    }

    static Stream<Arguments> invalidWeights() {
        return Stream.of(
                Arguments.of(-.1, .5, .3, .3),
                Arguments.of(.5, .5, .5, .5),
                Arguments.of(0, 0, 0, 0),
                Arguments.of(Double.NaN, .3, .1, .2),
                Arguments.of(Double.POSITIVE_INFINITY, 0, 0, 0),
                Arguments.of(1.1, -.1, 0, 0));
    }

    @ParameterizedTest
    @MethodSource("invalidWeights")
    void invalidBalancedConfigurationsAreRejected(double t, double c, double d, double e) {
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(t, c, d, e));
    }

    @Test
    void distanceStrategyStillEnforcesBudget() {
        var shortExpensive = TestData.simple("A", 1, 10, 2000, 1);
        var longerAffordable = TestData.simple("B", 5, 10, 100, 1);
        assertEquals("B", new DistanceFirstStrategy().selectRoute(
                List.of(shortExpensive, longerAffordable), TestData.request(500, 60, 10))
                .selectedRoute().id());
    }
}
```

## src/test/java/edu/assignment/delivery/TestData.java

```java
package edu.assignment.delivery;

import edu.assignment.delivery.demo.DemoData;
import edu.assignment.delivery.domain.*;

final class TestData {
    private TestData() {}

    static Route route(String id, double km, int minutes, double base, double rate,
                       double fee, double co2, int traffic, int weather, boolean available) {
        return new Route(id, DemoData.ORIGIN, DemoData.DESTINATION, km, minutes,
                base, rate, fee, co2, traffic, weather, available);
    }

    static Route simple(String id, double km, int minutes, double cost, double co2) {
        return route(id, km, minutes, cost, 0, 0, co2, 0, 0, true);
    }

    static DeliveryRequest request(double budget, int deadline, double emissions) {
        return DemoData.request("TEST", DeliveryPriority.STANDARD, budget, deadline, false, emissions);
    }
}
```
