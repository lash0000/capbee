package com.hydronode;

import com.hydronode.cli.Frames;
import com.hydronode.cli.Prompt;
import com.hydronode.cli.TablePrinter;
import com.hydronode.cli.Terminal;
import com.hydronode.cli.Theme;
import com.hydronode.dataset.ClassifiedDataset;
import com.hydronode.dataset.Dataset;
import com.hydronode.dataset.LeakDataset;
import com.hydronode.dataset.TrainableDataset;
import com.hydronode.dataset.Verdict;
import com.hydronode.leak.BattledimSensors;
import com.hydronode.leak.LeakBaseline;
import com.hydronode.leak.LeakCalculator;
import com.hydronode.leak.LeakComparison;
import com.hydronode.leak.LeakEvaluator;
import com.hydronode.leak.LeakValidation;
import com.hydronode.ml.Evaluation;
import com.hydronode.ml.RandomForest;
import com.hydronode.ml.TrainTestSplit;
import com.hydronode.ml.TrainingData;
import com.hydronode.ml.TreeOptions;
import com.hydronode.model.FieldValue;
import com.hydronode.model.Sample;
import com.hydronode.progress.ProgressListener;
import com.hydronode.schema.FieldSpec;
import com.hydronode.schema.SampleSchema;
import com.hydronode.service.SampleService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Screen flow. Every screen is drawn from scratch on the same grid, so the
 * console behaves like a small full-screen TUI; data handling stays in the
 * per-dataset service behind each {@link Track}.
 */
public final class Application {

    private static final int MENU_WIDTH = Frames.WIDTH - 2;

    private final Prompt prompt;
    private final List<Track> tracks;
    private final ProgressListener progress;
    private final Map<Integer, RandomForest> models = new HashMap<>();

    private int activeIndex;
    private String message;

    public Application(Prompt prompt, List<Track> tracks, ProgressListener progress) {
        this.prompt = prompt;
        this.tracks = List.copyOf(tracks);
        this.progress = progress;
    }

    public void run() {
        boolean running = true;
        while (running) {
            renderHome();
            int choice = prompt.askNumber("select", 1, 7);
            switch (choice) {
                case 1 -> createEntry();
                case 2 -> showHistory();
                case 3 -> showStatus();
                case 4 -> trainModel();
                case 5 -> leakFormulaTest();
                case 6 -> switchDataset();
                default -> running = false;
            }
        }
        renderGoodbye();
    }

    // ---------------------------------------------------------------- screens

    private void renderHome() {
        Terminal.clearScreen();
        Frames.wordmark();
        Frames.panel("Dataset", datasetLines());
        Frames.blank();
        Frames.panel("Main menu", menuLines());
        Frames.blank();

        if (message != null) {
            Frames.ok(message);
        }
        Frames.footer("1-7 select  ·  Enter to confirm  ·  Ctrl+C to quit");
    }

    private void createEntry() {
        renderKindScreen();
        int kind = prompt.askNumber("select", 1, 3);

        Sample sample;
        switch (kind) {
            case 1 -> {
                sample = createReal();
                if (sample == null) {
                    return;
                }
            }
            case 2 -> sample = service().save(service().synthetic());
            default -> sample = service().save(readManual());
        }

        message = "entry #" + service().count() + " saved  ·  " + service().count() + " total";
        showEntry(sample);
    }

    private Sample createReal() {
        Terminal.clearScreen();
        Frames.brand("Scanning dataset");
        Frames.blank();

        Optional<Sample> real = service().randomReal();
        if (real.isEmpty()) {
            Frames.warn("the dataset CSV is not available");
            Frames.blank();
            prompt.waitForEnter("press Enter to return");
            return null;
        }
        Terminal.pause(250L);
        return service().save(real.get());
    }

    private void renderKindScreen() {
        Terminal.clearScreen();
        Frames.brand("New test entry");
        Frames.blank();
        Frames.panel("Kind of sample", List.of(
                "",
                "   " + Theme.accent("[1]") + "   Real row from the dataset  " + Theme.muted("(random)"),
                "   " + Theme.accent("[2]") + "   Synthetic realistic row",
                "   " + Theme.accent("[3]") + "   Manual entry  " + Theme.muted("(you type every value)"),
                ""));
        Frames.blank();
    }

    private void showEntry(Sample sample) {
        Terminal.clearScreen();
        Frames.brand("Test data entry");
        Frames.blank();

        printVerdict(sample);
        TablePrinter.printRecord("Entry #" + service().count(), sample.fields());

        Frames.blank();
        prompt.waitForEnter("press Enter to continue");
        Frames.blank();
    }

    /** Shows the dataset's own answer for the sample, when it has one. */
    private void printVerdict(Sample sample) {
        if (!(dataset() instanceof ClassifiedDataset classified)) {
            return;
        }
        Verdict verdict = classified.verdict(sample);
        String mark = verdict.alert()
                ? Theme.warn("⚠  " + verdict.label())
                : Theme.ok("✔  " + verdict.label());

        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("   " + Theme.strong(mark));
        lines.add("   " + Theme.muted(verdict.detail()));

        String prediction = modelPrediction(sample);
        if (prediction != null) {
            lines.add("   " + prediction);
        }
        lines.add("");

        Frames.panel("Verdict", lines);
        Frames.blank();
    }

    /** The trained forest's opinion, when this dataset has a model. */
    private String modelPrediction(Sample sample) {
        RandomForest forest = models.get(activeIndex);
        if (forest == null || !(dataset() instanceof TrainableDataset trainable)) {
            return null;
        }

        double probability = forest.positiveProbability(trainable.featuresOf(sample));
        boolean positive = probability >= 0.5;
        double confidence = positive ? probability : 1.0 - probability;
        String label = positive ? "POTABLE" : "NOT POTABLE";

        return Theme.muted("model  ") + Theme.strong(label)
                + Theme.muted("  ·  " + Math.round(confidence * 100) + "% confident");
    }

    private Sample readManual() {
        SampleSchema schema = dataset().schema();

        Terminal.clearScreen();
        Frames.brand("Manual entry");
        Frames.blank();
        Frames.panel("Fill in the fields", Frames.wrap(
                "Bounds and options are read from the dataset itself. "
                        + schema.fields().size() + " fields to answer.", MENU_WIDTH));
        Frames.blank();

        List<FieldValue> values = new ArrayList<>();
        for (FieldSpec spec : schema.fields()) {
            values.add(new FieldValue(spec.name(), askValue(spec)));
        }
        Frames.blank();
        return new Sample(values);
    }

    private String askValue(FieldSpec spec) {
        if (spec.isNumber()) {
            double value = prompt.askDouble(spec.name(), spec.min(), spec.max());
            return String.format(Locale.ROOT, "%.2f", value);
        }
        if (spec.isText()) {
            return prompt.askText(spec.name());
        }
        List<String> options = spec.options();
        return options.get(prompt.askChoice(spec.name(), options));
    }

    private void showHistory() {
        Terminal.clearScreen();
        Frames.brand("Saved entries");
        Frames.blank();

        List<Sample> entries = service().history();
        if (entries.isEmpty()) {
            Frames.panel("Saved entries", List.of("", "   " + Theme.muted("no entries yet"), ""));
        } else {
            List<String> columns = dataset().schema().summaryColumns();
            List<String[]> rows = new ArrayList<>();
            for (Sample entry : entries) {
                rows.add(entry.toRow(columns));
            }
            TablePrinter.print("Saved entries", columns, rows);
        }

        Frames.blank();
        prompt.waitForEnter("press Enter to return");
        Frames.blank();
    }

    private void trainModel() {
        if (!(dataset() instanceof TrainableDataset trainable)) {
            Terminal.clearScreen();
            Frames.brand("Train model");
            Frames.blank();
            Frames.panel("Not available", List.of(
                    "",
                    "   " + Theme.muted("this dataset has no binary label to train on"),
                    ""));
            Frames.blank();
            prompt.waitForEnter("press Enter to return");
            return;
        }

        Optional<TrainingData> optional = trainable.trainingData();
        if (optional.isEmpty()) {
            Terminal.clearScreen();
            Frames.brand("Train model");
            Frames.blank();
            Frames.warn("the dataset has no usable rows");
            Frames.blank();
            prompt.waitForEnter("press Enter to return");
            return;
        }

        TrainingData data = optional.get();
        TrainTestSplit split = TrainTestSplit.holdout(data, 0.7, 42L);
        TreeOptions options = TreeOptions.defaults(data.featureCount());

        Terminal.clearScreen();
        Frames.brand("Training random forest");
        Frames.blank();

        long start = System.nanoTime();
        RandomForest forest = new RandomForest();
        forest.train(split.train(), options, progress);
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;

        Evaluation evaluation = Evaluation.of(forest, split.test(), 0.5);
        models.put(activeIndex, forest);
        message = "model trained · accuracy " + metric(evaluation.accuracy());

        Terminal.clearScreen();
        Frames.brand("Model trained");
        Frames.blank();
        Frames.panel("Model", modelLines(options, split, seconds));
        Frames.blank();
        Frames.panel("Evaluation · hold-out", evaluationLines(evaluation));
        Frames.blank();
        prompt.waitForEnter("press Enter to return");
        Frames.blank();
    }

    private List<String> modelLines(TreeOptions options, TrainTestSplit split, double seconds) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(Frames.row("algorithm", "Random Forest · pure Java"));
        lines.add(Frames.row("trees", String.valueOf(options.trees())));
        lines.add(Frames.row("splits", options.featuresPerSplit() + " of "
                + split.train().featureCount() + " features per split"));
        lines.add(Frames.row("train", String.format(Locale.ROOT, "%,d rows", split.train().size())));
        lines.add(Frames.row("test", String.format(Locale.ROOT, "%,d rows", split.test().size())));
        lines.add(Frames.row("trained", String.format(Locale.ROOT, "%.2f s", seconds)));
        lines.add("");
        return lines;
    }

    private List<String> evaluationLines(Evaluation evaluation) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(Frames.row("accuracy", metric(evaluation.accuracy())));
        lines.add(Frames.row("precision", metric(evaluation.precision())));
        lines.add(Frames.row("recall", metric(evaluation.recall())));
        lines.add(Frames.row("f1", metric(evaluation.f1())));
        lines.add(Frames.row("confusion", "TP " + evaluation.truePositives()
                + " · FP " + evaluation.falsePositives()
                + " · TN " + evaluation.trueNegatives()
                + " · FN " + evaluation.falseNegatives()));
        lines.add("");
        return lines;
    }

    private static String metric(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private void leakFormulaTest() {
        Terminal.clearScreen();
        Frames.brand("Leak formula test");
        Frames.blank();

        if (!(dataset() instanceof LeakDataset)) {
            Frames.panel("Not available", List.of(
                    "",
                    "   " + Theme.muted("switch to the LeakDB dataset first  [6]"),
                    ""));
            Frames.blank();
            prompt.waitForEnter("press Enter to return");
            return;
        }

        BattledimSensors sensors = new BattledimSensors();
        if (!sensors.isAvailable()) {
            Frames.warn("sensor files are missing");
            Frames.blank();
            Frames.panel("Needed", Frames.wrap(sensors.hint(), MENU_WIDTH));
            Frames.blank();
            prompt.waitForEnter("press Enter to return");
            return;
        }

        Frames.panel("Calibration", List.of(
                "",
                "   " + Theme.muted("taking medians of the leak-free rows ..."),
                ""));

        LeakBaseline baseline = LeakEvaluator.calibrate(sensors, 20_000);
        LeakCalculator calculator = new LeakCalculator(baseline, 0.05);
        double[] thresholds = {0.25, 0.5, 1.0, 2.0, 5.0, 10.0};

        Terminal.clearScreen();
        Frames.brand("Leak formula test");
        Frames.blank();
        LeakComparison comparison = LeakEvaluator.sweep(sensors, calculator, thresholds, 12, progress);

        Terminal.clearScreen();
        Frames.brand("Leak formula test");
        Frames.blank();
        Frames.panel("Baseline · leak-free rows", baselineLines(baseline));
        Frames.blank();
        Frames.panel("Formula", formulaLines());
        Frames.blank();
        TablePrinter.print("Raw readings", sweepHeaders(), sweepRows(comparison.raw()));
        TablePrinter.print("Moving average · window " + comparison.window(),
                sweepHeaders(), sweepRows(comparison.smoothed()));
        Frames.ok(conclusion(comparison));
        Frames.blank();
        prompt.waitForEnter("press Enter to return");
        Frames.blank();
    }

    private List<String> baselineLines(LeakBaseline baseline) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(Frames.row("Q0 flow", String.format(Locale.ROOT, "%.2f L/s", baseline.flow())));
        lines.add(Frames.row("P0 press", String.format(Locale.ROOT, "%.2f m", baseline.pressure())));
        lines.add(Frames.row("samples", String.format(Locale.ROOT, "%,d healthy rows", baseline.samples())));
        lines.add("");
        return lines;
    }

    private List<String> formulaLines() {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("   " + Theme.muted("leak rate   ") + "L = Q - Q0");
        lines.add("   " + Theme.muted("severity    ") + "SI = (P0 - P) / P0 x 100");
        lines.add("   " + Theme.muted("rule        ") + "L > 5% of Q0  AND  SI > threshold");
        lines.add("");
        return lines;
    }

    private List<String> sweepHeaders() {
        return List.of("SI threshold", "flagged", "precision", "recall", "f1");
    }

    private List<String[]> sweepRows(List<LeakComparison.ThresholdResult> results) {
        List<String[]> rows = new ArrayList<>();
        for (LeakComparison.ThresholdResult result : results) {
            LeakValidation validation = result.validation();
            int flagged = validation.truePositives() + validation.falsePositives();
            rows.add(new String[] {
                    String.format(Locale.ROOT, "%.2f %%", result.severityThresholdPercent()),
                    String.format(Locale.ROOT, "%,d", flagged),
                    metric(validation.precision()),
                    metric(validation.recall()),
                    metric(validation.f1())});
        }
        return rows;
    }

    private String conclusion(LeakComparison comparison) {
        int atTen = flaggedAt(comparison.raw(), 10.0);
        LeakComparison.ThresholdResult best = bestByF1(comparison.raw());
        if (best == null) {
            return "no threshold flagged anything on this data";
        }
        return String.format(Locale.ROOT,
                "textbook SI>10%% flags %,d rows  ·  best f1 %.3f at SI>%.2f%%",
                atTen, best.validation().f1(), best.severityThresholdPercent());
    }

    private static int flaggedAt(List<LeakComparison.ThresholdResult> results, double threshold) {
        return results.stream()
                .filter(result -> result.severityThresholdPercent() == threshold)
                .findFirst()
                .map(result -> result.validation().truePositives() + result.validation().falsePositives())
                .orElse(0);
    }

    private static LeakComparison.ThresholdResult bestByF1(List<LeakComparison.ThresholdResult> results) {
        LeakComparison.ThresholdResult best = null;
        for (LeakComparison.ThresholdResult result : results) {
            int flagged = result.validation().truePositives() + result.validation().falsePositives();
            if (flagged == 0) {
                continue;
            }
            if (best == null || result.validation().f1() > best.validation().f1()) {
                best = result;
            }
        }
        return best;
    }

    private void switchDataset() {
        Terminal.clearScreen();
        Frames.brand("Switch dataset");
        Frames.blank();

        List<String> lines = new ArrayList<>();
        lines.add("");
        for (int i = 0; i < tracks.size(); i++) {
            Dataset candidate = tracks.get(i).dataset();
            String marker = i == activeIndex ? Theme.ok("●") : Theme.muted("○");
            String suffix = candidate.isAvailable() ? "" : "  " + Theme.muted("(no data)");
            lines.add("   " + marker + "  " + Theme.accent("[" + (i + 1) + "]") + "  "
                    + candidate.displayName() + suffix);
        }
        lines.add("");
        Frames.panel("Available datasets", lines);
        Frames.blank();

        int choice = prompt.askNumber("select", 1, tracks.size());
        activeIndex = choice - 1;
        message = "switched to " + dataset().displayName();
    }

    private void showStatus() {
        Terminal.clearScreen();
        Frames.brand("Dataset status");
        Frames.blank();

        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(Frames.row("name", dataset().displayName()));
        lines.add(Frames.row("columns", String.valueOf(dataset().columns().size())));
        lines.add(Frames.row("rows", String.format(Locale.ROOT, "%,d", dataset().rowCount())));
        lines.add(Frames.row("source", dataset().sourceName()));
        lines.add(Frames.row("available", dataset().isAvailable() ? "yes" : "no"));
        lines.add(Frames.row("saved", String.valueOf(service().count())));
        lines.add("");
        Frames.panel("Dataset", lines);

        Frames.blank();
        List<String> columnLines = new ArrayList<>();
        columnLines.add("");
        for (String line : Frames.wrap(String.join("  ·  ", dataset().columns()), MENU_WIDTH - 2)) {
            columnLines.add("   " + line);
        }
        columnLines.add("");
        Frames.panel("Columns", columnLines);

        if (!dataset().isAvailable()) {
            Frames.blank();
            List<String> hintLines = new ArrayList<>();
            hintLines.add("");
            for (String line : Frames.wrap(dataset().availabilityHint(), MENU_WIDTH - 2)) {
                hintLines.add("   " + line);
            }
            hintLines.add("");
            Frames.panel("How to get the data", hintLines);
        }

        Frames.blank();
        prompt.waitForEnter("press Enter to return");
        Frames.blank();
    }

    private void renderGoodbye() {
        Terminal.clearScreen();
        Frames.wordmark();
        Frames.blank();

        int saved = tracks.stream().mapToInt(track -> track.service().count()).sum();
        Frames.ok("bye · " + saved + " entries created this session");
        Frames.blank();
    }

    // ----------------------------------------------------------------- blocks

    private List<String> datasetLines() {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(Frames.row("dataset", dataset().displayName()));
        lines.add(Frames.row("columns", String.valueOf(dataset().columns().size())));
        lines.add(Frames.row("rows", String.format(Locale.ROOT, "%,d", dataset().rowCount())));
        lines.add(Frames.row("source", dataset().isAvailable() ? dataset().sourceName() : "not downloaded"));
        lines.add("");
        return lines;
    }

    private List<String> menuLines() {
        return List.of(
                "",
                "   " + Theme.accent("[1]") + "   Create a test data entry",
                "   " + Theme.accent("[2]") + "   View saved entries  " + Theme.muted("(" + service().count() + ")"),
                "   " + Theme.accent("[3]") + "   Dataset status",
                "   " + Theme.accent("[4]") + "   Train & evaluate model  "
                        + Theme.muted(modelStatus()),
                "   " + Theme.accent("[5]") + "   Leak formula test  "
                        + Theme.muted(dataset() instanceof LeakDataset ? "(L, SI, validation)" : "(LeakDB only)"),
                "   " + Theme.accent("[6]") + "   Switch dataset  " + Theme.muted("(" + tracks.size() + ")"),
                "   " + Theme.accent("[7]") + "   Exit",
                "");
    }

    private String modelStatus() {
        if (!(dataset() instanceof TrainableDataset)) {
            return "(needs a binary label)";
        }
        RandomForest forest = models.get(activeIndex);
        return forest == null ? "(not trained)" : "(" + forest.treeCount() + " trees)";
    }

    private Dataset dataset() {
        return tracks.get(activeIndex).dataset();
    }

    private SampleService service() {
        return tracks.get(activeIndex).service();
    }
}
