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
