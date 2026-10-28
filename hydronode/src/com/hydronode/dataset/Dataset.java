package com.hydronode.dataset;

import com.hydronode.schema.SampleSchema;

import java.util.List;

/**
 * Contract for a tabular dataset. Swapping in another notebook's data
 * (e.g. BattLeDIM/LeakDB) means writing a new implementation of this
 * interface — the CLI, the service and the printer stay untouched.
 */
public interface Dataset {

    String displayName();

    String description();

    /** Columns of the dataset, whether or not the CSV is present. */
    List<String> columns();

    boolean isAvailable();

    /** Short path/name of the data source, used in progress labels. */
    String sourceName();

    /** Human-readable instructions shown when the CSV is missing. */
    String availabilityHint();

    /** Reads at most {@code maxRows} data rows into memory. */
    CsvTable read(int maxRows);

    /** Total number of data rows, used for progress percentages. */
    long rowCount();

    /**
     * Streams every data row without holding the file in memory.
     *
     * @return the number of rows handed to the handler
     */
    long streamRows(RowHandler handler);

    /**
     * Column types, numeric bounds and choice options used to build entries.
     * Derived from real values when the CSV is present, from declared
     * fallbacks otherwise.
     */
    SampleSchema schema();
}
