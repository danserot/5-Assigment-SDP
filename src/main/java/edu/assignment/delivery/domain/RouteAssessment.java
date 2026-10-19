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
