package com.hydronode.ml;

import com.hydronode.progress.ProgressListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Random Forest: many CART trees, each grown on a bootstrap sample with a
 * random feature subset per split, combined by averaging their leaf
 * probabilities (soft voting).
 *
 * <p>Missing values are imputed with the per-column median of the training
 * rows, computed once at fit time.</p>
 */
public final class RandomForest {

    private final List<DecisionTree> trees = new ArrayList<>();

    private double[] medians = new double[0];

    public void train(TrainingData data, TreeOptions options, ProgressListener progress) {
        int size = data.size();
        medians = mediansOf(data.rows());
        double[][] features = impute(data.rows(), medians);
        int[] labels = data.labels();
        Random random = new Random(options.seed());

        progress.start("training random forest", options.trees());

        int[] positiveIndices = indicesWhere(labels, 1);
        int[] negativeIndices = indicesWhere(labels, 0);
        boolean canBalance = positiveIndices.length > 0 && negativeIndices.length > 0;

        for (int t = 0; t < options.trees(); t++) {
            double[][] sampleFeatures = new double[size][];
            int[] sampleLabels = new int[size];
            for (int i = 0; i < size; i++) {
                int pick;
                if (canBalance) {
                    // Draw both classes evenly so the minority class is not
                    // drowned out by the prior (0/1 labels are rarely balanced).
                    pick = random.nextBoolean()
                            ? positiveIndices[random.nextInt(positiveIndices.length)]
                            : negativeIndices[random.nextInt(negativeIndices.length)];
                } else {
                    pick = random.nextInt(size);
                }
                sampleFeatures[i] = features[pick];
                sampleLabels[i] = labels[pick];
            }

            DecisionTree tree = new DecisionTree();
            tree.train(sampleFeatures, sampleLabels, options, random);
            trees.add(tree);
            progress.update(t + 1);
        }
        progress.finish(options.trees());
    }

    /** Average positive-class probability across all trees. */
    public double positiveProbability(double[] row) {
        if (trees.isEmpty()) {
            return 0.0;
        }
        double[] imputed = imputeRow(row);
        double sum = 0.0;
        for (DecisionTree tree : trees) {
            sum += tree.positiveProbability(imputed);
        }
        return sum / trees.size();
    }

    public int treeCount() {
        return trees.size();
    }

    private static int[] indicesWhere(int[] labels, int value) {
        int count = 0;
        for (int label : labels) {
            if (label == value) {
                count++;
            }
        }
        int[] result = new int[count];
        int index = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == value) {
                result[index++] = i;
            }
        }
        return result;
    }

    public double medianOf(int feature) {
        return medians[feature];
    }

    // -------------------------------------------------------------- imputation

    private double[] imputeRow(double[] row) {
        double[] values = row.clone();
        for (int i = 0; i < values.length; i++) {
            if (Double.isNaN(values[i])) {
                values[i] = medians[i];
            }
        }
        return values;
    }

    private double[][] impute(double[][] rows, double[] fill) {
        double[][] result = new double[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            double[] values = rows[r].clone();
            for (int c = 0; c < values.length; c++) {
                if (Double.isNaN(values[c])) {
                    values[c] = fill[c];
                }
            }
            result[r] = values;
        }
        return result;
    }

    private static double[] mediansOf(double[][] rows) {
        int columns = rows[0].length;
        double[] medians = new double[columns];
        double[] buffer = new double[rows.length];

        for (int c = 0; c < columns; c++) {
            int count = 0;
            for (double[] row : rows) {
                if (!Double.isNaN(row[c])) {
                    buffer[count++] = row[c];
                }
            }
            if (count == 0) {
                medians[c] = 0.0;
                continue;
            }
            Arrays.sort(buffer, 0, count);
            medians[c] = buffer[count / 2];
        }
        return medians;
    }
}
