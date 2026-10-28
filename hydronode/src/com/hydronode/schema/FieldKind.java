package com.hydronode.schema;

/** How a column is filled in when building a test entry. */
public enum FieldKind {

    /** Numeric column: validated against the observed min/max. */
    NUMBER,

    /** Textual column: chosen from the observed distinct values. */
    CHOICE,

    /** Free text, typed by the user (timestamps and similar). */
    TEXT
}
