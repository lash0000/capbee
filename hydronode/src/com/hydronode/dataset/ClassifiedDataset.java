package com.hydronode.dataset;

/**
 * A dataset that can say something about a single sample, e.g. whether a pipe
 * reading indicates a leak. Kept separate from {@link Dataset} so plain
 * datasets do not have to implement it.
 */
public interface ClassifiedDataset extends Dataset {

    Verdict verdict(com.hydronode.model.Sample sample);
}
