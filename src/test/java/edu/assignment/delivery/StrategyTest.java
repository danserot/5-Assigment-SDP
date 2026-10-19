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
