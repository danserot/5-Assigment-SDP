package edu.assignment.delivery.domain;

public record Location(String name, double latitude, double longitude) {
    public Location {
        Validation.text(name, "name");
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid geographic coordinates");
        }
    }
}
