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
