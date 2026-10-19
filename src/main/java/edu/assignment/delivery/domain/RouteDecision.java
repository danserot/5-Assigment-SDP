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
