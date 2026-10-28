package com.hydronode.ml;

import java.util.Random;

/** Shuffled hold-out split so the reported score is on unseen rows. */
public record TrainTestSplit(TrainingData train, TrainingData test) {

    public static TrainTestSplit holdout(TrainingData data, double trainRatio, long seed) {
        int size = data.size();
        int[] order = new int[size];
        for (int i = 0; i < size; i++) {
            order[i] = i;
        }

        Random random = new Random(seed);
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = order[i];
            order[i] = order[j];
            order[j] = swap;
        }

        int trainSize = (int) Math.round(size * trainRatio);
        trainSize = Math.max(1, Math.min(size - 1, trainSize));

        double[][] trainRows = new double[trainSize][];
        int[] trainLabels = new int[trainSize];
        double[][] testRows = new double[size - trainSize][];
        int[] testLabels = new int[size - trainSize];

        for (int i = 0; i < trainSize; i++) {
            trainRows[i] = data.rows()[order[i]];
            trainLabels[i] = data.labels()[order[i]];
        }
        for (int i = trainSize; i < size; i++) {
            testRows[i - trainSize] = data.rows()[order[i]];
            testLabels[i - trainSize] = data.labels()[order[i]];
        }

        return new TrainTestSplit(
                new TrainingData(data.features(), trainRows, trainLabels),
                new TrainingData(data.features(), testRows, testLabels));
    }
}
