package com.hydronode.leak;

/**
 * Result of applying the formula to one reading.
 *
 * @param leakRate      {@code L = Q - Q0} (positive = more inflow than nominal)
 * @param severityIndex {@code SI = (P0 - P) / P0 * 100} (positive = pressure drop)
 * @param severity      SMALL / MEDIUM / LARGE from the severity index
 */
public record LeakAssessment(
        double flow,
        double pressure,
        double leakRate,
        double severityIndex,
        String severity) {
}
