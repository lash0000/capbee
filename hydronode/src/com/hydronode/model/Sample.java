package com.hydronode.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One dataset row: an ordered list of named values. Order follows the schema
 * (or the CSV header), so printing never needs to look anything up twice.
 */
public record Sample(List<FieldValue> fields) {

    /** Placeholder printed when a field has no value. */
    public static final String UNKNOWN = "—";

    public Sample {
        fields = List.copyOf(fields);
    }

    public String value(String column) {
        for (FieldValue field : fields) {
            if (field.name().equalsIgnoreCase(column)) {
                return field.value();
            }
        }
        return null;
    }

    /** Returns a copy with one column replaced (or appended when absent). */
    public Sample withValue(String column, String newValue) {
        List<FieldValue> updated = new ArrayList<>(fields);
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).name().equalsIgnoreCase(column)) {
                updated.set(i, updated.get(i).withValue(newValue));
                return new Sample(updated);
            }
        }
        updated.add(0, new FieldValue(column, newValue));
        return new Sample(updated);
    }

    /** Projects this sample onto the given column order, for table output. */
    public String[] toRow(List<String> columns) {
        String[] row = new String[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            String raw = value(columns.get(i));
            row[i] = raw == null || raw.isBlank() ? UNKNOWN : raw;
        }
        return row;
    }

    /** Builds a sample straight from a CSV row, keeping every column. */
    public static Sample fromCsvRow(List<String> headers, String[] row) {
        List<FieldValue> values = new ArrayList<>(headers.size());
        for (int i = 0; i < headers.size(); i++) {
            String raw = i < row.length && row[i] != null ? row[i].trim() : "";
            values.add(new FieldValue(headers.get(i), raw));
        }
        return new Sample(values);
    }
}
