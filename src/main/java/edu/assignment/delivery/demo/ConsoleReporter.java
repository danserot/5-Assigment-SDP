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
