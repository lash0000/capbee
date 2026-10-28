package com.hydronode.dataset;

import com.hydronode.ml.TrainingData;
import com.hydronode.model.Sample;

import java.util.Optional;

/**
 * A dataset that can be turned into a supervised training matrix. Kept
 * separate from {@link Dataset} so plain CSV datasets stay simple.
 */
public interface TrainableDataset extends Dataset {

    /** Numeric matrix plus binary labels, or empty when the data is missing. */
    Optional<TrainingData> trainingData();

    /** Projects a single sample onto the model's feature order (NaN if blank). */
    double[] featuresOf(Sample sample);
}
