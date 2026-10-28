package com.hydronode.dataset;

import com.hydronode.model.Sample;
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
import java.util.Locale;

/**
 * BattLeDIM / LeakDB leakage file, as used by the water-leak notebook.
 *
 * <p>The CSV is one row per timestamp with a column per pipe, separated by
 * {@code ;} and using a comma as decimal separator. This class reshapes it into
 * one virtual row per <em>pipe reading</em>, because a whole timestamp is a poor
 * unit: pipe {@code p257} carries a leak through 93% of 2018, so "any pipe
 * leaking" would answer yes almost every time. Per pipe the leak rate is about
 * 12%, which is a question worth asking.</p>
 */
public final class LeakDataset implements ClassifiedDataset {

    private static final char DELIMITER = CsvReader.SEMICOLON;
    private static final double LEAK_THRESHOLD = 5.0;
    private static final double MAX_LEAK_VALUE = 40.0;
    private static final int TIMESTAMP_SCAN_LINES = 4_000;
    private static final int TIMESTAMP_STRIDE = 160;

    private static final List<String> COLUMNS = List.of("Timestamp", "Pipe", "Leak_Value");

    private static final List<Path> CANDIDATE_PATHS = List.of(
            Path.of("data", "battledim", "BattLeDIM", "2018_Leakages.csv"),
            Path.of("data", "BattLeDIM", "2018_Leakages.csv"));

    private static final String HINT = """
            The leak readings are not on disk yet. Download them once:
              kaggle datasets download larserikrisholm/battledim -p data
              unzip -o data/battledim.zip -d data
            Expected file: data/battledim/BattLeDIM/2018_Leakages.csv""";

    private final Path resolvedPath;

    private List<String> cachedPipes;
    private List<String> cachedTimestamps;
    private Long cachedRowCount;

    public LeakDataset() {
        this.resolvedPath = CANDIDATE_PATHS.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String displayName() {
        return "LeakDB / BattLeDIM (water leak)";
    }

    @Override
    public String description() {
        return "Per-pipe leakage readings from the L-Town network at 5-minute resolution (2018).";
    }

    @Override
    public List<String> columns() {
        return List.copyOf(COLUMNS);
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
    public SampleSchema schema() {
        return new SampleSchema(List.of(
                FieldSpec.text("Timestamp", sampleTimestamps()),
                FieldSpec.choice("Pipe", pipes()),
                FieldSpec.number("Leak_Value", 0, MAX_LEAK_VALUE)), COLUMNS);
    }

    @Override
    public CsvTable read(int maxRows) {
        List<String[]> rows = new ArrayList<>();
        stream(maxRows, (columns, index, row) -> rows.add(row));
        return new CsvTable(COLUMNS, rows);
    }

    @Override
    public long streamRows(RowHandler handler) {
        return stream(-1, handler);
    }

    @Override
    public long rowCount() {
        if (cachedRowCount == null) {
            cachedRowCount = countDataLines() * pipes().size();
        }
        return cachedRowCount;
    }

    @Override
    public Verdict verdict(Sample sample) {
        double value = parse(sample.value("Leak_Value"));
        if (Double.isNaN(value)) {
            return new Verdict("UNKNOWN", "no reading for this pipe", false);
        }

        boolean leak = value > LEAK_THRESHOLD;
        String detail = String.format(Locale.ROOT,
                "reading %.2f against a threshold of %.1f", value, LEAK_THRESHOLD);
        return leak
                ? new Verdict("LEAK", detail, true)
                : new Verdict("NO LEAK", detail, false);
    }

    // ------------------------------------------------------------------ reading

    private long stream(int maxRows, RowHandler handler) {
        if (resolvedPath == null) {
            return 0L;
        }
        try (BufferedReader reader = Files.newBufferedReader(resolvedPath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return 0L;
            }
            List<String> headers = CsvReader.parseLine(CsvReader.stripBom(headerLine), DELIMITER);

            long index = 0L;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cells = CsvReader.parseLine(line, DELIMITER);
                String timestamp = cells.get(0);

                for (int i = 1; i < headers.size() && i < cells.size(); i++) {
                    if (maxRows >= 0 && index >= maxRows) {
                        return index;
                    }
                    String[] row = {
                            timestamp,
                            headers.get(i),
                            CsvReader.normalizeNumber(cells.get(i))
                    };
                    handler.accept(COLUMNS, index, row);
                    index++;
                }
            }
            return index;
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to read " + resolvedPath, ex);
        }
    }

    /**
     * A handful of real timestamps spread over the first few thousand rows, so
     * a synthetic entry carries a plausible time instead of a made-up string.
     */
    private List<String> sampleTimestamps() {
        if (cachedTimestamps != null) {
            return cachedTimestamps;
        }

        List<String> values = new ArrayList<>();
        if (resolvedPath != null) {
            try (BufferedReader reader = Files.newBufferedReader(resolvedPath, StandardCharsets.UTF_8)) {
                reader.readLine(); // header
                String line;
                int scanned = 0;
                while (scanned < TIMESTAMP_SCAN_LINES && (line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    if (scanned % TIMESTAMP_STRIDE == 0) {
                        int end = line.indexOf(DELIMITER);
                        values.add(end > 0 ? line.substring(0, end) : line);
                    }
                    scanned++;
                }
            } catch (IOException ex) {
                throw new UncheckedIOException("failed to sample timestamps from " + resolvedPath, ex);
            }
        }

        cachedTimestamps = List.copyOf(values);
        return cachedTimestamps;
    }

    private List<String> pipes() {        if (cachedPipes == null) {
            List<String> headers = header();
            cachedPipes = List.copyOf(headers.subList(Math.min(1, headers.size()), headers.size()));
        }
        return cachedPipes;
    }

    private List<String> header() {
        if (resolvedPath == null) {
            return List.of("Timestamp");
        }
        try (BufferedReader reader = Files.newBufferedReader(resolvedPath, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            if (line == null) {
                return List.of("Timestamp");
            }
            return CsvReader.parseLine(CsvReader.stripBom(line), DELIMITER);
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to read header " + resolvedPath, ex);
        }
    }

    private long countDataLines() {
        if (resolvedPath == null) {
            return 0L;
        }
        try (BufferedReader reader = Files.newBufferedReader(resolvedPath, StandardCharsets.UTF_8)) {
            if (reader.readLine() == null) {
                return 0L;
            }
            long lines = 0L;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines++;
                }
            }
            return lines;
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to count rows in " + resolvedPath, ex);
        }
    }

    private static double parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(raw.replace(',', '.'));
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }
}
