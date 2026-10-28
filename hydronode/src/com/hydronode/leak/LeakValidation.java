package com.hydronode.leak;

/** Confusion matrix for "the formula said leak" vs the dataset label. */
public record LeakValidation(int truePositives, int falsePositives, int trueNegatives, int falseNegatives) {

    public int total() {
        return truePositives + falsePositives + trueNegatives + falseNegatives;
    }

    public double accuracy() {
        return total() == 0 ? 0 : (double) (truePositives + trueNegatives) / total();
    }

    public double precision() {
        int flagged = truePositives + falsePositives;
        return flagged == 0 ? 0 : (double) truePositives / flagged;
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

    /** Share of all "leak" decisions that were wrong. */
    public double falseAlarmRate() {
        int flagged = truePositives + falsePositives;
        return flagged == 0 ? 0 : (double) falsePositives / flagged;
    }
}
