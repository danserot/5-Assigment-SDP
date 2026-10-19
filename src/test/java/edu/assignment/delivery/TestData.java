package edu.assignment.delivery;

import edu.assignment.delivery.demo.DemoData;
import edu.assignment.delivery.domain.*;

final class TestData {
    private TestData() {}

    static Route route(String id, double km, int minutes, double base, double rate,
                       double fee, double co2, int traffic, int weather, boolean available) {
        return new Route(id, DemoData.ORIGIN, DemoData.DESTINATION, km, minutes,
                base, rate, fee, co2, traffic, weather, available);
    }

    static Route simple(String id, double km, int minutes, double cost, double co2) {
        return route(id, km, minutes, cost, 0, 0, co2, 0, 0, true);
    }

    static DeliveryRequest request(double budget, int deadline, double emissions) {
        return DemoData.request("TEST", DeliveryPriority.STANDARD, budget, deadline, false, emissions);
    }
}
