package com.hydronode.ml;

import java.util.List;

/**
 * A supervised training matrix: one row per sample, one column per feature,
 * plus the binary label.
 *
 * <p>Missing readings are represented as {@link Double#NaN}; the trainer
 * imputes them from the training rows only, so the test split never leaks
 * information back into the model.</p>
 */
public record TrainingData(List<String> features, double[][] rows, int[] labels) {

    public TrainingData {
        features = List.copyOf(features);
    }

    public int size() {
        return rows.length;
    }

    public int featureCount() {
        return features.size();
    }

    public int positives() {
        int total = 0;
        for (int label : labels) {
            total += label;
        }
        return total;
    }
}
