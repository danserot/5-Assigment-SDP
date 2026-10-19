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
