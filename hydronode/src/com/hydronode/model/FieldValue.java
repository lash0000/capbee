package com.hydronode.model;

/** One column name paired with its value for a single sample. */
public record FieldValue(String name, String value) {

    public FieldValue withValue(String newValue) {
        return new FieldValue(name, newValue);
    }
}
