package com.hydronode.cli;

import com.hydronode.model.FieldValue;

import java.util.List;
import java.util.Locale;

/**
 * Box tables on the same grid as {@link Frames}: every table is exactly
 * {@link Frames#WIDTH} wide, with the title embedded in the top border.
 */
public final class TablePrinter {

    private static final int MAX_COLUMN_WIDTH = 26;
    private static final long ROW_DELAY_MILLIS = 30L;

    private TablePrinter() {
    }

    /** Horizontal table: one row per sample. Used for the history view. */
    public static void print(String title, List<String> headers, List<String[]> rows) {
        int[] widths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            int widest = headers.get(i).length();
            for (String[] row : rows) {
                if (i < row.length && row[i] != null) {
                    widest = Math.max(widest, row[i].length());
                }
            }
            widths[i] = Math.min(widest, MAX_COLUMN_WIDTH);
        }
        stretch(widths);

        System.out.println("  " + topBorder(widths, title));
        System.out.println("  " + cells(widths, headers.toArray(String[]::new), true));
        System.out.println("  " + border(widths, '├', '┼', '┤'));
        for (String[] row : rows) {
            System.out.println("  " + cells(widths, row, false));
        }
        System.out.println("  " + border(widths, '└', '┴', '┘'));
        System.out.println("  " + Theme.muted(rows.size() + " row(s)"));
    }

    /**
     * Vertical table: one field per line. Rows appear one at a time on a real
     * terminal so the entry looks like it is being filled in.
     */
    public static void printRecord(String title, List<FieldValue> fields) {
        int nameWidth = 0;
        int valueWidth = 0;
        for (FieldValue field : fields) {
            nameWidth = Math.max(nameWidth, field.name().length());
            valueWidth = Math.max(valueWidth, display(field).length());
        }
        int[] widths = {Math.min(nameWidth, 26), Math.min(valueWidth, 30)};
        stretch(widths);

        boolean animated = Terminal.isInteractive();
        System.out.println("  " + topBorder(widths, title));

        for (FieldValue field : fields) {
            System.out.println("  " + cells(widths, new String[] {field.name(), display(field)}, false));
            System.out.flush();
            if (animated) {
                Terminal.pause(ROW_DELAY_MILLIS);
            }
        }

        System.out.println("  " + border(widths, '└', '┴', '┘'));
        System.out.println("  " + Theme.muted(fields.size() + " field(s)"));
    }

    private static void stretch(int[] widths) {
        int target = Frames.WIDTH + 2;
        int current = totalWidth(widths);
        if (current < target) {
            widths[widths.length - 1] += target - current;
        }
    }

    private static int totalWidth(int[] widths) {
        int total = 1;
        for (int width : widths) {
            total += width + 3;
        }
        return total;
    }

    private static String topBorder(int[] widths, String title) {
        int room = totalWidth(widths) - 2;
        if (title == null || title.isBlank()) {
            return Theme.border("┌" + "─".repeat(room) + "┐");
        }
        String label = "─ " + title.toUpperCase(Locale.ROOT) + " ";
        if (label.length() > room - 1) {
            label = label.substring(0, Math.max(2, room - 2)) + "… ";
        }
        String fill = "─".repeat(Math.max(0, room - label.length()));
        return Theme.border("┌") + Theme.title(label) + Theme.border(fill + "┐");
    }

    /** Draws a border; {@code mid} is the junction character between columns. */
    private static String border(int[] widths, char left, char mid, char right) {
        StringBuilder builder = new StringBuilder(String.valueOf(left));
        for (int i = 0; i < widths.length; i++) {
            builder.append("─".repeat(widths[i] + 2));
            if (i < widths.length - 1) {
                builder.append(mid);
            }
        }
        builder.append(right);
        return Theme.border(builder.toString());
    }

    private static String cells(int[] widths, String[] values, boolean header) {
        StringBuilder builder = new StringBuilder(Theme.border("│"));
        for (int i = 0; i < widths.length; i++) {
            String raw = i < values.length && values[i] != null ? values[i] : "";
            String shown = fit(raw, widths[i]);
            String padded = shown + " ".repeat(Math.max(0, widths[i] - shown.length()));
            builder.append(' ')
                    .append(header ? Theme.title(padded) : padded)
                    .append(' ')
                    .append(Theme.border("│"));
        }
        return builder.toString();
    }

    private static String display(FieldValue field) {
        String raw = field.value();
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        return tidy(raw);
    }

    /** Trims long float noise for display; the stored value is untouched. */
    private static String tidy(String raw) {
        int dot = raw.indexOf('.');
        if (dot < 0 || raw.length() - dot <= 5) {
            return raw;
        }
        try {
            double value = Double.parseDouble(raw);
            String shown = String.format(Locale.ROOT, "%.4f", value);
            int end = shown.length();
            while (end > 0 && shown.charAt(end - 1) == '0') {
                end--;
            }
            if (end > 0 && shown.charAt(end - 1) == '.') {
                end++;
            }
            return shown.substring(0, end);
        } catch (NumberFormatException ex) {
            return raw;
        }
    }

    private static String fit(String value, int width) {
        if (value.length() <= width) {
            return value;
        }
        return width <= 1 ? "…" : value.substring(0, width - 1) + "…";
    }
}
