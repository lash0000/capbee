package com.hydronode.schema;

import java.util.List;

/**
 * Describes one column of the dataset: its name, whether it is numeric or
 * categorical, the numeric bounds, or the allowed options.
 *
 * <p>Instances are built by {@code IrrigationDataset} from real CSV values, so
 * the ranges and choices are always the dataset's own.</p>
 */
public record FieldSpec(
        String name,
        FieldKind kind,
        double min,
        double max,
        List<String> options) {

    public FieldSpec {
        options = List.copyOf(options);
    }

    public static FieldSpec number(String name, double min, double max) {
        return new FieldSpec(name, FieldKind.NUMBER, min, max, List.of());
    }

    public static FieldSpec choice(String name, List<String> options) {
        List<String> safe = options.isEmpty() ? List.of("") : options;
        return new FieldSpec(name, FieldKind.CHOICE, 0, 0, safe);
    }

    /**
     * Free text typed by the user. {@code samples} are real values observed in
     * the dataset; the synthetic generator picks from them instead of inventing
     * something meaningless.
     */
    public static FieldSpec text(String name, List<String> samples) {
        return new FieldSpec(name, FieldKind.TEXT, 0, 0, samples);
    }

    public boolean isNumber() {
        return kind == FieldKind.NUMBER;
    }

    public boolean isText() {
        return kind == FieldKind.TEXT;
    }
}
