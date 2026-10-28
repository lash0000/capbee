package com.hydronode.dataset;

import com.hydronode.schema.FieldSpec;
import com.hydronode.schema.SampleSchema;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Derives a schema from real rows: a column whose values all parse as numbers
 * becomes a bounded NUMBER field, anything else becomes a CHOICE over its
 * observed values. Shared by every CSV dataset so the behaviour is identical.
 */
public final class SchemaInference {

    private static final int MAX_OPTIONS = 20;
    private static final int LOW_CARDINALITY_LIMIT = 3;

    private SchemaInference() {
    }

    /**
     * @param skipped lower-case column names to leave out of the form (ids,
     *                derived columns)
     */
    public static SampleSchema infer(CsvTable table, Set<String> skipped, List<String> summaryColumns) {
        if (table.isEmpty()) {
            return new SampleSchema(List.of(), summaryColumns);
        }

        List<String> headers = table.headers();
        List<FieldSpec> specs = new ArrayList<>();

        for (int i = 0; i < headers.size(); i++) {
            String column = headers.get(i);
            if (skipped.contains(column.toLowerCase(Locale.ROOT))) {
                continue;
            }

            List<String> values = new ArrayList<>();
            for (String[] row : table.rows()) {
                if (i < row.length && !row[i].isBlank()) {
                    values.add(row[i]);
                }
            }
            specs.add(specFor(column, values));
        }
        return new SampleSchema(specs, summaryColumns);
    }

    private static FieldSpec specFor(String column, List<String> values) {
        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
        boolean numeric = true;

        for (String value : values) {
            try {
                statistics.accept(Double.parseDouble(value.replace(',', '.')));
            } catch (NumberFormatException ex) {
                numeric = false;
                break;
            }
        }

        if (numeric && statistics.getCount() > 0) {
            List<String> distinct = values.stream().distinct().toList();
            // A numeric column with a couple of values (0/1 labels) behaves like
            // a category: offer it as a choice instead of a free number.
            if (distinct.size() <= LOW_CARDINALITY_LIMIT) {
                return FieldSpec.choice(column, distinct.stream().sorted().toList());
            }
            return FieldSpec.number(column, roundDown(statistics.getMin()), roundUp(statistics.getMax()));
        }

        List<String> options = values.stream()
                .distinct()
                .sorted()
                .limit(MAX_OPTIONS)
                .toList();
        return FieldSpec.choice(column, options);
    }

    private static double roundDown(double value) {
        return Math.floor(value * 100.0) / 100.0;
    }

    private static double roundUp(double value) {
        return Math.ceil(value * 100.0) / 100.0;
    }
}
