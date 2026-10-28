package com.hydronode.ml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * CART decision tree with Gini impurity, grown on a subset of features at each
 * split. Only what the forest needs: binary labels, numeric features, no
 * pruning.
 */
public final class DecisionTree {

    private Node root;

    public void train(double[][] features, int[] labels, TreeOptions options, Random random) {
        int[] indices = new int[features.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        root = build(features, labels, indices, 0, options, random);
    }

    /** Share of the training rows in the reached leaf that were positive. */
    public double positiveProbability(double[] row) {
        Node node = root;
        while (!node.leaf) {
            node = row[node.feature] <= node.threshold ? node.left : node.right;
        }
        return node.probability;
    }

    private Node build(double[][] features, int[] labels, int[] indices, int depth,
            TreeOptions options, Random random) {
        Node node = new Node();
        int positives = 0;
        for (int index : indices) {
            positives += labels[index];
        }
        node.probability = indices.length == 0 ? 0.0 : (double) positives / indices.length;
        node.prediction = node.probability >= 0.5 ? 1 : 0;

        boolean pure = positives == 0 || positives == indices.length;
        if (depth >= options.maxDepth()
                || indices.length < options.minLeafSamples() * 2
                || pure) {
            node.leaf = true;
            return node;
        }

        Split split = bestSplit(features, labels, indices, options, random);
        if (split == null) {
            node.leaf = true;
            return node;
        }

        int leftSize = 0;
        for (int index : indices) {
            if (features[index][split.feature] <= split.threshold) {
                leftSize++;
            }
        }
        int[] left = new int[leftSize];
        int[] right = new int[indices.length - leftSize];
        int l = 0;
        int r = 0;
        for (int index : indices) {
            if (features[index][split.feature] <= split.threshold) {
                left[l++] = index;
            } else {
                right[r++] = index;
            }
        }

        node.feature = split.feature;
        node.threshold = split.threshold;
        node.left = build(features, labels, left, depth + 1, options, random);
        node.right = build(features, labels, right, depth + 1, options, random);
        return node;
    }

    private Split bestSplit(double[][] features, int[] labels, int[] indices,
            TreeOptions options, Random random) {
        int featureCount = features[0].length;
        List<Integer> candidates = new ArrayList<>(featureCount);
        for (int feature = 0; feature < featureCount; feature++) {
            candidates.add(feature);
        }
        Collections.shuffle(candidates, random);

        int considered = Math.min(options.featuresPerSplit(), featureCount);
        Split best = null;
        double bestScore = Double.MAX_VALUE;

        for (int c = 0; c < considered; c++) {
            int feature = candidates.get(c);

            int[] order = indices.clone();
            sortByFeature(order, features, feature);

            int total = order.length;
            int totalPositives = 0;
            for (int index : order) {
                totalPositives += labels[index];
            }
            int totalNegatives = total - totalPositives;

            int leftPositives = 0;
            int leftNegatives = 0;

            for (int k = 0; k < total - 1; k++) {
                int index = order[k];
                if (labels[index] == 1) {
                    leftPositives++;
                } else {
                    leftNegatives++;
                }

                double value = features[index][feature];
                double next = features[order[k + 1]][feature];
                if (value == next) {
                    continue;
                }

                int leftCount = k + 1;
                int rightCount = total - leftCount;
                if (leftCount < options.minLeafSamples() || rightCount < options.minLeafSamples()) {
                    continue;
                }

                double score = gini(leftPositives, leftNegatives, leftCount) * leftCount / total
                        + gini(totalPositives - leftPositives, totalNegatives - leftNegatives, rightCount)
                                * rightCount / total;
                if (score < bestScore) {
                    bestScore = score;
                    best = new Split(feature, (value + next) / 2.0);
                }
            }
        }
        return best;
    }

    private static double gini(int positives, int negatives, int total) {
        if (total == 0) {
            return 0.0;
        }
        double p = (double) positives / total;
        double q = (double) negatives / total;
        return 1.0 - p * p - q * q;
    }

    private static void sortByFeature(int[] order, double[][] features, int feature) {
        sort(order, features, feature, 0, order.length - 1);
    }

    /** Iterative-tail quicksort on indices; no boxing, no extra arrays. */
    private static void sort(int[] order, double[][] features, int feature, int low, int high) {
        int lo = low;
        int hi = high;
        while (lo < hi) {
            double pivot = features[order[(lo + hi) >>> 1]][feature];
            int i = lo;
            int j = hi;
            while (i <= j) {
                while (features[order[i]][feature] < pivot) {
                    i++;
                }
                while (features[order[j]][feature] > pivot) {
                    j--;
                }
                if (i <= j) {
                    int swap = order[i];
                    order[i] = order[j];
                    order[j] = swap;
                    i++;
                    j--;
                }
            }
            if (j - lo < hi - i) {
                sort(order, features, feature, lo, j);
                lo = i;
            } else {
                sort(order, features, feature, i, hi);
                hi = j;
            }
        }
    }

    private static final class Node {
        private boolean leaf;
        private int prediction;
        private double probability;
        private int feature;
        private double threshold;
        private Node left;
        private Node right;
    }

    private record Split(int feature, double threshold) {
    }
}
