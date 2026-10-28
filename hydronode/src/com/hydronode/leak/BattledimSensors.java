package com.hydronode.leak;

import com.hydronode.dataset.CsvReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads the three BattLeDIM files in lockstep. They share the same timestamp
 * grid (105,120 rows at 5-minute resolution), so row N of each file describes
 * the same moment and no join is needed.
 */
public final class BattledimSensors {

    private static final char DELIMITER = CsvReader.SEMICOLON;
    private static final double PIPE_LEAK_THRESHOLD = 5.0;

    private static final List<Path> ROOTS = List.of(
            Path.of("data", "battledim", "BattLeDIM"),
            Path.of("data", "BattLeDIM"));

    private final Path flows;
    private final Path pressures;
    private final Path leakages;

    private Long cachedRowCount;

    public BattledimSensors() {
        Path root = ROOTS.stream().filter(Files::isDirectory).findFirst().orElse(null);
        this.flows = root == null ? null : root.resolve("2018_SCADA_Flows.csv");
        this.pressures = root == null ? null : root.resolve("2018_SCADA_Pressures.csv");
        this.leakages = root == null ? null : root.resolve("2018_Leakages.csv");
    }

    public boolean isAvailable() {
        return flows != null && Files.isRegularFile(flows)
                && pressures != null && Files.isRegularFile(pressures)
                && leakages != null && Files.isRegularFile(leakages);
    }

    public String sourceName() {
        return flows == null ? "(no csv)" : flows.getParent().toString();
    }

    public String hint() {
        return "Needs three files in data/battledim/BattLeDIM/: flows, pressures and leakages.";
    }

    public long rowCount() {
        if (cachedRowCount == null) {
            cachedRowCount = countRows();
        }
        return cachedRowCount;
    }

    /** @return the number of aligned rows handed to the consumer */
    public long stream(ReadingConsumer consumer) {
        if (!isAvailable()) {
            return 0L;
        }
        try (BufferedReader flowReader = Files.newBufferedReader(flows, StandardCharsets.UTF_8);
                BufferedReader pressureReader = Files.newBufferedReader(pressures, StandardCharsets.UTF_8);
                BufferedReader leakReader = Files.newBufferedReader(leakages, StandardCharsets.UTF_8)) {

            flowReader.readLine();
            pressureReader.readLine();
            leakReader.readLine();

            long rows = 0L;
            String flowLine;
            while ((flowLine = flowReader.readLine()) != null) {
                String pressureLine = pressureReader.readLine();
                String leakLine = leakReader.readLine();
                if (pressureLine == null || leakLine == null) {
                    break;
                }
                if (flowLine.isBlank()) {
                    continue;
                }

                List<String> flowCells = CsvReader.parseLine(flowLine, DELIMITER);
                List<String> pressureCells = CsvReader.parseLine(pressureLine, DELIMITER);
                List<String> leakCells = CsvReader.parseLine(leakLine, DELIMITER);

                consumer.accept(new LeakReading(
                        flowCells.get(0),
                        number(flowCells, 1),
                        mean(pressureCells, 1),
                        anyAbove(leakCells, 1, PIPE_LEAK_THRESHOLD)));
                rows++;
            }
            return rows;
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to read BattLeDIM sensors", ex);
        }
    }

    private long countRows() {
        if (flows == null || !Files.isRegularFile(flows)) {
            return 0L;
        }
        try (BufferedReader reader = Files.newBufferedReader(flows, StandardCharsets.UTF_8)) {
            if (reader.readLine() == null) {
                return 0L;
            }
            long rows = 0L;
            while (reader.readLine() != null) {
                rows++;
            }
            return rows;
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to count " + flows, ex);
        }
    }

    private static double mean(List<String> cells, int from) {
        double sum = 0.0;
        int count = 0;
        for (int i = from; i < cells.size(); i++) {
            double value = parse(cells.get(i));
            if (!Double.isNaN(value)) {
                sum += value;
                count++;
            }
        }
        return count == 0 ? Double.NaN : sum / count;
    }

    private static double number(List<String> cells, int index) {
        return index < cells.size() ? parse(cells.get(index)) : Double.NaN;
    }

    private static boolean anyAbove(List<String> cells, int from, double threshold) {
        for (int i = from; i < cells.size(); i++) {
            if (parse(cells.get(i)) > threshold) {
                return true;
            }
        }
        return false;
    }

    private static double parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    @FunctionalInterface
    public interface ReadingConsumer {
        void accept(LeakReading reading);
    }
}
