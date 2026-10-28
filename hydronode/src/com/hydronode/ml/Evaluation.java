package com.hydronode.ml;

/**
 * Confusion-matrix view of a binary classifier.
 *
 * <p>Class 1 is the positive class — here "potable".</p>
 */
public record Evaluation(int truePositives, int falsePositives, int trueNegatives, int falseNegatives) {

    public static Evaluation of(RandomForest forest, TrainingData data, double threshold) {
        int tp = 0;
        int fp = 0;
        int tn = 0;
        int fn = 0;

        for (int r = 0; r < data.size(); r++) {
            double probability = forest.positiveProbability(data.rows()[r]);
            boolean predictedPositive = probability >= threshold;
            boolean actualPositive = data.labels()[r] == 1;

            if (predictedPositive && actualPositive) {
                tp++;
            } else if (predictedPositive) {
                fp++;
            } else if (actualPositive) {
                fn++;
            } else {
                tn++;
            }
        }
        return new Evaluation(tp, fp, tn, fn);
    }

    public int total() {
        return truePositives + falsePositives + trueNegatives + falseNegatives;
    }

    public double accuracy() {
        return total() == 0 ? 0 : (double) (truePositives + trueNegatives) / total();
    }

    public double precision() {
        int predicted = truePositives + falsePositives;
        return predicted == 0 ? 0 : (double) truePositives / predicted;
    }

    public double recall() {
        int actual = truePositives + falseNegatives;
        return actual == 0 ? 0 : (double) truePositives / actual;
    }

    public double f1() {
        double precision = precision();
        double recall = recall();
        return precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
    }
}
