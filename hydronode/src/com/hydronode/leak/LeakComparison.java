package com.hydronode.leak;

import java.util.List;

/**
 * Raw formula versus the same formula on a moving average, each swept across
 * several severity thresholds — the point being that the textbook 10% rule is
 * meaningless here and the data itself shows where the useful range sits.
 */
public record LeakComparison(
        LeakBaseline baseline,
        List<ThresholdResult> raw,
        List<ThresholdResult> smoothed,
        int window,
        long rows) {

    public LeakComparison {
        raw = List.copyOf(raw);
        smoothed = List.copyOf(smoothed);
    }

    /** One row of the sweep. */
    public record ThresholdResult(double severityThresholdPercent, LeakValidation validation) {
    }
}
