package com.hydronode.dataset;

import java.util.List;

/**
 * A parsed slice of a CSV file: the header row plus the data rows that were
 * kept. Keeping headers alongside rows means consumers can map by name rather
 * than by position.
 */
public record CsvTable(List<String> headers, List<String[]> rows) {

    public CsvTable {
        headers = List.copyOf(headers);
        rows = List.copyOf(rows);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
