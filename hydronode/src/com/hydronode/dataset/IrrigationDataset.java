package com.hydronode.dataset;

import com.hydronode.schema.FieldSpec;
import com.hydronode.schema.SampleSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Kaggle playground-series-s6e4 ("Irrigation Need") dataset, read from a local
 * CSV export. The schema (numeric ranges, category options) is inferred from
 * the file itself, so the CLI always offers values the dataset actually
 * contains.
 */
public final class IrrigationDataset implements Dataset {

    private static final List<Path> CANDIDATE_PATHS = List.of(
            Path.of("data", "train.csv"),
            Path.of("data", "playground-series-s6e4", "train.csv"),
            Path.of("train.csv"));

    private static final int INFERENCE_ROWS = 2_000;

    private static final String HINT = """
            The dataset CSV is not on disk yet. Download it once with the Kaggle CLI:
              kaggle datasets download zia068/playground-series-s6e4 -f train.csv -p data
            (That is a public mirror of the competition files, so no competition
            entry is needed. The official alternative is
            `kaggle competitions download -c playground-series-s6e4`, which does
            require accepting the competition rules.)
            Expected file location: data/train.csv""";

    /** Columns used for the compact history table. */
    private static final List<String> SUMMARY_COLUMNS = List.of(
            "id", "Temperature_C", "Rainfall_mm", "Crop_Type", "Irrigation_Need");

    /** Schema used when the CSV is absent, taken from the notebook's feature set. */
    private static final List<String> FALLBACK_COLUMNS = List.of(
            "id", "Temperature_C", "Humidity", "Rainfall_mm",
            "Previous_Irrigation_mm", "Crop_Type", "Soil_Type", "Irrigation_Need");

    private final Path resolvedPath;
    private SampleSchema cachedSchema;
    private long cachedRowCount = -1L;

    public IrrigationDataset() {
        this.resolvedPath = CANDIDATE_PATHS.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String displayName() {
        return "Irrigation Need (playground-series-s6e4)";
    }

    @Override
    public String description() {
        return "Crop, soil and weather readings used to predict the Irrigation_Need label.";
    }

    @Override
    public List<String> columns() {
        if (!isAvailable()) {
            return FALLBACK_COLUMNS;
        }
        List<String> headers = read(1).headers();
        return headers.isEmpty() ? FALLBACK_COLUMNS : headers;
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
            return new CsvTable(FALLBACK_COLUMNS, List.of());
        }
        try (BufferedReader reader = Files.newBufferedReader(resolvedPath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new CsvTable(FALLBACK_COLUMNS, List.of());
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
    public long rowCount() {
        if (cachedRowCount < 0) {
            cachedRowCount = countDataRows();
        }
        return cachedRowCount;
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
    public SampleSchema schema() {
        if (cachedSchema == null) {
            cachedSchema = buildSchema();
        }
        return cachedSchema;
    }

    private SampleSchema buildSchema() {
        CsvTable table = read(INFERENCE_ROWS);
        if (table.isEmpty()) {
            return fallbackSchema();
        }
        return SchemaInference.infer(table, Set.of("id"), summaryColumns(table.headers()));
    }

    private static SampleSchema fallbackSchema() {
        List<FieldSpec> specs = List.of(
                FieldSpec.number("Temperature_C", 5, 45),
                FieldSpec.number("Humidity", 20, 100),
                FieldSpec.number("Rainfall_mm", 0, 300),
                FieldSpec.number("Previous_Irrigation_mm", 0, 200),
                FieldSpec.choice("Crop_Type", List.of("Maize", "Rice", "Sugarcane", "Wheat")),
                FieldSpec.choice("Soil_Type", List.of("Clayey", "Loamy", "Sandy")),
                FieldSpec.choice("Irrigation_Need", List.of("High", "Low", "Medium")));
        return new SampleSchema(specs, SUMMARY_COLUMNS);
    }

    private static List<String> summaryColumns(List<String> available) {
        List<String> present = SUMMARY_COLUMNS.stream()
                .filter(column -> containsIgnoreCase(available, column))
                .toList();
        return present.isEmpty() ? available : present;
    }

    private static boolean containsIgnoreCase(List<String> values, String target) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(target));
    }
}
