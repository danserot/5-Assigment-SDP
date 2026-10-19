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
