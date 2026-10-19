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
