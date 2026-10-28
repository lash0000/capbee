package com.hydronode.leak;

import com.hydronode.progress.ProgressListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runs the textbook formula against the whole year and scores it with the
 * ground-truth labels, sweeping the severity threshold — so the "basic
 * calculation is noisy" claim becomes a number instead of an opinion.
 */
public final class LeakEvaluator {

    private LeakEvaluator() {
    }

    /** Medians over rows the dataset labels as leak-free. */
    public static LeakBaseline calibrate(BattledimSensors sensors, int maxHealthyRows) {
        List<Double> flows = new ArrayList<>();
        List<Double> pressures = new ArrayList<>();

        sensors.stream(reading -> {
            if (!reading.actualLeak() && flows.size() < maxHealthyRows
                    && !Double.isNaN(reading.flow()) && !Double.isNaN(reading.pressure())) {
                flows.add(reading.flow());
                pressures.add(reading.pressure());
            }
        });

        return new LeakBaseline(median(flows), median(pressures), flows.size());
    }

    public static LeakComparison sweep(BattledimSensors sensors, LeakCalculator calculator,
            double[] severityThresholds, int window, ProgressListener progress) {

        Accumulator[] raw = accumulators(severityThresholds.length);
        Accumulator[] smoothed = accumulators(severityThresholds.length);

        double[] flowWindow = new double[window];
        double[] pressureWindow = new double[window];

        long[] position = {0L};
        long[] rows = {0L};

        progress.start("evaluating " + sensors.sourceName(), sensors.rowCount());
        sensors.stream(reading -> {
            LeakAssessment rawAssessment = calculator.assess(reading.flow(), reading.pressure());

            flowWindow[(int) (position[0] % window)] = reading.flow();
            pressureWindow[(int) (position[0] % window)] = reading.pressure();
            position[0]++;

            int filled = (int) Math.min(position[0], window);
            LeakAssessment smoothedAssessment = calculator.assess(
                    average(flowWindow, filled), average(pressureWindow, filled));

            for (int i = 0; i < severityThresholds.length; i++) {
                raw[i].add(calculator.flags(rawAssessment, severityThresholds[i]), reading.actualLeak());
                smoothed[i].add(calculator.flags(smoothedAssessment, severityThresholds[i]),
                        reading.actualLeak());
            }

            rows[0]++;
            progress.update(rows[0]);
        });
        progress.finish(rows[0]);

        return new LeakComparison(calculator.baseline(), results(raw, severityThresholds),
                results(smoothed, severityThresholds), window, rows[0]);
    }

    private static Accumulator[] accumulators(int size) {
        Accumulator[] accumulators = new Accumulator[size];
        for (int i = 0; i < size; i++) {
            accumulators[i] = new Accumulator();
        }
        return accumulators;
    }

    private static List<LeakComparison.ThresholdResult> results(Accumulator[] accumulators,
            double[] thresholds) {
        List<LeakComparison.ThresholdResult> results = new ArrayList<>(thresholds.length);
        for (int i = 0; i < thresholds.length; i++) {
            results.add(new LeakComparison.ThresholdResult(thresholds[i], accumulators[i].toValidation()));
        }
        return results;
    }

    private static double average(double[] buffer, int filled) {
        if (filled == 0) {
            return Double.NaN;
        }
        double sum = 0.0;
        for (int i = 0; i < filled; i++) {
            sum += buffer[i];
        }
        return sum / filled;
    }

    private static double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    private static final class Accumulator {

        private int truePositives;
        private int falsePositives;
        private int trueNegatives;
        private int falseNegatives;

        void add(boolean detected, boolean actual) {
            if (detected && actual) {
                truePositives++;
            } else if (detected) {
                falsePositives++;
            } else if (actual) {
                falseNegatives++;
            } else {
                trueNegatives++;
            }
        }

        LeakValidation toValidation() {
            return new LeakValidation(truePositives, falsePositives, trueNegatives, falseNegatives);
        }
    }
}
