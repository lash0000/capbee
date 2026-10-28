package com.hydronode.schema;

import java.util.List;

/**
 * Ordered column definitions for one dataset, plus the short column list used
 * by the history view.
 */
public record SampleSchema(List<FieldSpec> fields, List<String> summaryColumns) {

    public SampleSchema {
        fields = List.copyOf(fields);
        summaryColumns = List.copyOf(summaryColumns);
    }
}
