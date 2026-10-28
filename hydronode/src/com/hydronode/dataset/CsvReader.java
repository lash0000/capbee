package com.hydronode.dataset;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal CSV field splitter: handles double quotes, escaped quotes ("") and
 * delimiters inside quoted fields. The delimiter is configurable because
 * BattLeDIM ships semicolon-separated files.
 */
public final class CsvReader {

    public static final char COMMA = ',';
    public static final char SEMICOLON = ';';

    private CsvReader() {
    }

    public static List<String> parseLine(String line) {
        return parseLine(line, COMMA);
    }

    public static List<String> parseLine(String line, char delimiter) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);

            if (quoted) {
                if (character == '"') {
                    boolean escaped = i + 1 < line.length() && line.charAt(i + 1) == '"';
                    if (escaped) {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(character);
                }
            } else if (character == '"') {
                quoted = true;
            } else if (character == delimiter) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        fields.add(current.toString().trim());
        return fields;
    }

    /** European exports use a comma as decimal separator; Java needs a dot. */
    public static String normalizeNumber(String raw) {
        return raw == null ? null : raw.replace(',', '.');
    }

    static String stripBom(String line) {
        return !line.isEmpty() && line.charAt(0) == '\uFEFF' ? line.substring(1) : line;
    }
}
