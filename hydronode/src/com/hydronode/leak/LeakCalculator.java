package com.hydronode.leak;

/**
 * The textbook formulas, with one correction this dataset forced on us.
 *
 * <p>The usual leak rate is {@code L = Q0 - Q}, assuming the measured flow
 * drops when water escapes. In BattLeDIM the flow sensor sits on the network
 * inflow: when a pipe leaks, the pumps push <em>more</em> water in, so healthy
 * rows average 75.2 L/s and leaking rows average 99.7 L/s. The sign is
 * therefore flipped here — {@code L = Q - Q0}.</p>
 *
 * <p>Detection itself is left to the caller, because the severity threshold is
 * exactly what needs testing: on this network no sensor drops more than ~2%,
 * so the textbook "SI &gt; 10%" rule never fires.</p>
 */
public record LeakCalculator(LeakBaseline baseline, double flowTolerance) {

    public LeakAssessment assess(double flow, double pressure) {
        double leakRate = flow - baseline.flow();
        double drop = baseline.pressure() - pressure;
        double severityIndex = baseline.pressure() == 0 ? 0 : (drop / baseline.pressure()) * 100.0;
        return new LeakAssessment(flow, pressure, leakRate, severityIndex, severity(severityIndex));
    }

    /** Flow must exceed tolerance <em>and</em> pressure must drop past the threshold. */
    public boolean flags(LeakAssessment assessment, double severityThresholdPercent) {
        return assessment.leakRate() > baseline.flow() * flowTolerance
                && assessment.severityIndex() > severityThresholdPercent;
    }

    /** Thresholds from the brief: small &lt; 5%, medium 5–15%, large &gt; 15%. */
    public static String severity(double severityIndex) {
        double magnitude = Math.abs(severityIndex);
        if (magnitude < 5.0) {
            return "SMALL";
        }
        if (magnitude < 15.0) {
            return "MEDIUM";
        }
        return "LARGE";
    }
}
