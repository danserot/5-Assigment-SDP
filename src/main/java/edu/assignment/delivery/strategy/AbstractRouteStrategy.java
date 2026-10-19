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
