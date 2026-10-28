package com.hydronode.dataset;

import com.hydronode.ml.TrainingData;
import com.hydronode.model.Sample;
import com.hydronode.schema.SampleSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Drinking-water potability dataset used by the water-quality notebook
 * (Random Forest baseline). 3,276 rows, 9 chemical features and a 0/1
 * {@code Potability} label.
 *
 * <p>Some readings are missing in the source file (ph, Sulfate,
 * Trihalomethanes); they are shown as an em dash rather than invented.</p>
 */
public final class PotabilityDataset implements ClassifiedDataset, TrainableDataset {

    private static final List<Path> CANDIDATE_PATHS = List.of(
            Path.of("data", "potability", "water_potability.csv"),
            Path.of("data", "water_potability.csv"));

    private static final String LABEL = "Potability";

    private static final List<String> SUMMARY_COLUMNS = List.of(
            "ph", "Hardness", "Solids", "Chloramines", "Turbidity", "Potability");

    private static final String HINT = """
            The potability file is not on disk yet. Download it once:
              kaggle datasets download adityakadiwal/water-potability -p data/potability
              unzip -o data/potability/water-potability.zip -d data/potability
            Expected file: data/potability/water_potability.csv""";

    private final Path resolvedPath;
    private SampleSchema cachedSchema;
    private long cachedRowCount = -1L;

    public PotabilityDataset() {
        this.resolvedPath = CANDIDATE_PATHS.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String displayName() {
        return "Water Potability (drinking water)";
    }

    @Override
    public String description() {
        return "Chemical readings of water samples labelled potable or not, the Random Forest baseline dataset.";
    }

    @Override
    public List<String> columns() {
        if (!isAvailable()) {
            return List.of("ph", "Hardness", "Solids", "Chloramines", "Sulfate",
                    "Conductivity", "Organic_carbon", "Trihalomethanes", "Turbidity", "Potability");
        }
        List<String> headers = read(1).headers();
        return headers.isEmpty() ? SUMMARY_COLUMNS : headers;
    }

    @Override
    public boolean isAvailable() {
        return resolvedPath != null;
    }

    @Override
    public String sourceName() {
        return resolvedPath == null ? "(no csv)" : resolvedPath.toString();
    }

    @Override
    public String availabilityHint() {
        return resolvedPath != null
                ? "Reading from " + resolvedPath.toAbsolutePath()
                : HINT;
    }

    @Override
    public CsvTable read(int maxRows) {
        if (resolvedPath == null) {
            return new CsvTable(columns(), List.of());
        }
        try (BufferedReader reader = Files.newBufferedReader(resolvedPath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new CsvTable(columns(), List.of());
            }

            List<String> headers = CsvReader.parseLine(CsvReader.stripBom(headerLine));
            List<String[]> rows = new ArrayList<>();
            String line;
            while (rows.size() < maxRows && (line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                rows.add(CsvReader.parseLine(line).toArray(String[]::new));
            }
            return new CsvTable(headers, rows);
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to read " + resolvedPath, ex);
        }
    }

    @Override
    public long streamRows(RowHandler handler) {
        if (resolvedPath == null) {
            return 0L;
        }
        try (BufferedReader reader = Files.newBufferedReader(resolvedPath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return 0L;
            }
            List<String> headers = CsvReader.parseLine(CsvReader.stripBom(headerLine));

            long index = 0L;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                handler.accept(headers, index, CsvReader.parseLine(line).toArray(String[]::new));
                index++;
            }
            return index;
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to stream " + resolvedPath, ex);
        }
    }

    @Override
    public long rowCount() {
        if (cachedRowCount < 0) {
            cachedRowCount = countDataRows();
        }
        return cachedRowCount;
    }

    @Override
    public SampleSchema schema() {
        if (cachedSchema == null) {
            CsvTable table = read(4_000);
            cachedSchema = table.isEmpty()
                    ? new SampleSchema(List.of(), SUMMARY_COLUMNS)
                    : SchemaInference.infer(table, Set.of(), summaryColumns(table.headers()));
        }
        return cachedSchema;
    }

    @Override
    public Verdict verdict(Sample sample) {
        String raw = sample.value("Potability");
        if (raw == null || raw.isBlank()) {
            return new Verdict("UNKNOWN", "the label is missing for this row", false);
        }

        double value;
        try {
            value = Double.parseDouble(raw.trim());
        } catch (NumberFormatException ex) {
            return new Verdict("UNKNOWN", "unrecognised Potability value: " + raw.trim(), false);
        }

        return value >= 0.5
                ? new Verdict("POTABLE", "Potability = 1 (safe to drink)", false)
                : new Verdict("NOT POTABLE", "Potability = 0 (unsafe to drink)", true);
    }

    private long countDataRows() {
        if (resolvedPath == null) {
            return 0L;
        }
        try (BufferedReader reader = Files.newBufferedReader(resolvedPath, StandardCharsets.UTF_8)) {
            if (reader.readLine() == null) {
                return 0L;
            }
            long rows = 0L;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    rows++;
                }
            }
            return rows;
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to count rows in " + resolvedPath, ex);
        }
    }

    private static List<String> summaryColumns(List<String> available) {
        List<String> present = SUMMARY_COLUMNS.stream()
                .filter(column -> available.stream().anyMatch(value -> value.equalsIgnoreCase(column)))
                .toList();
        return present.isEmpty() ? available : present;
    }

    // ------------------------------------------------------------ training data

    @Override
    public Optional<TrainingData> trainingData() {
        if (!isAvailable()) {
            return Optional.empty();
        }
        CsvTable table = read(4_000);
        if (table.isEmpty()) {
            return Optional.empty();
        }

        List<String> headers = table.headers();
        int labelIndex = indexOf(headers, LABEL);
        if (labelIndex < 0) {
            return Optional.empty();
        }

        List<String> features = featureNames(headers);
        List<double[]> rows = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        for (String[] row : table.rows()) {
            double label = parse(row, labelIndex);
            if (Double.isNaN(label)) {
                continue;
            }
            double[] values = new double[features.size()];
            int column = 0;
            for (int i = 0; i < headers.size(); i++) {
                if (i == labelIndex) {
                    continue;
                }
                values[column++] = parse(row, i);
            }
            rows.add(values);
            labels.add(label >= 0.5 ? 1 : 0);
        }

        int[] labelArray = labels.stream().mapToInt(Integer::intValue).toArray();
        return Optional.of(
                new TrainingData(features, rows.toArray(double[][]::new), labelArray));
    }

    @Override
    public double[] featuresOf(Sample sample) {
        List<String> features = featureNames(columns());
        double[] values = new double[features.size()];
        for (int i = 0; i < features.size(); i++) {
            String raw = sample.value(features.get(i));
            values[i] = raw == null || raw.isBlank() ? Double.NaN : parseValue(raw);
        }
        return values;
    }

    private static List<String> featureNames(List<String> headers) {
        int labelIndex = indexOf(headers, LABEL);
        List<String> features = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            if (i != labelIndex) {
                features.add(headers.get(i));
            }
        }
        return features;
    }

    private static int indexOf(List<String> headers, String column) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).equalsIgnoreCase(column)) {
                return i;
            }
        }
        return -1;
    }

    private static double parse(String[] row, int index) {
        if (index < 0 || index >= row.length) {
            return Double.NaN;
        }
        return parseValue(row[index]);
    }

    private static double parseValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }
}
