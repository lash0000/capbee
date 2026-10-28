package com.hydronode.ml;

/**
 * Hyper-parameters for one forest.
 *
 * @param trees           number of decision trees
 * @param maxDepth        maximum depth per tree
 * @param minLeafSamples  smallest allowed leaf
 * @param featuresPerSplit features considered at each split ({@code mtry})
 * @param seed            seed so a run is reproducible
 */
public record TreeOptions(
        int trees,
        int maxDepth,
        int minLeafSamples,
        int featuresPerSplit,
        long seed) {

    /** sqrt(features) is the usual default for classification. */
    public static TreeOptions defaults(int featureCount) {
        int mtry = Math.max(1, (int) Math.round(Math.sqrt(featureCount)));
        return new TreeOptions(40, 10, 3, mtry, 42L);
    }
}
