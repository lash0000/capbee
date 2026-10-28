package com.hydronode.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The visual language of the console: one fixed panel width, inline panel
 * titles, a block wordmark and footer hints. Keeping every screen on the same
 * grid is what makes it feel like a TUI instead of scattered printouts.
 */
public final class Frames {

    /** Inner width of every panel; total line length is this plus 2. */
    public static final int WIDTH = 72;

    private static final int WORDMARK_ROWS = 6;

    private static final Map<Character, List<String>> LETTERS = letters();

    private Frames() {
    }

    /** Big block wordmark, printed once on the home screen. */
    public static void wordmark() {
        List<String> rows = blockRows("HYDRO");
        for (String row : rows) {
            System.out.println("  " + Theme.title(row));
        }
        System.out.println("  " + Theme.muted("N O D E") + Theme.muted("   ·   dataset test-entry console"));
        System.out.println();
    }

    /** Compact one-line brand used on the inner screens. */
    public static void brand(String screen) {
        System.out.println();
        System.out.println("  " + Theme.title("◈ HYDRONODE") + Theme.muted("  ·  ") + Theme.strong(screen));
    }

    /** Box with the title embedded in the top border. */
    public static void panel(String title, List<String> lines) {
        System.out.println("  " + top(title));
        for (String line : lines) {
            System.out.println("  " + body(line));
        }
        System.out.println("  " + bottom());
    }

    public static String top(String title) {
        String label = "─ " + title.toUpperCase(Locale.ROOT) + " ";
        int fill = Math.max(0, WIDTH - label.length());
        return Theme.border("┌") + Theme.title(label) + Theme.border("─".repeat(fill) + "┐");
    }

    public static String bottom() {
        return Theme.border("└" + "─".repeat(WIDTH) + "┘");
    }

    public static String body(String content) {
        int visible = visibleLength(content);
        String shown = visible <= WIDTH - 2 ? content : fit(content, WIDTH - 2);
        return Theme.border("│") + " " + shown
                + " ".repeat(Math.max(0, WIDTH - 2 - visible)) + " "
                + Theme.border("│");
    }

    /** Length of a string once ANSI color codes are removed. */
    public static int visibleLength(String text) {
        return text.replaceAll("\u001b\\[[0-9;]*m", "").length();
    }

    /** Dim hint line printed under the panels. */
    public static void footer(String hint) {
        System.out.println();
        System.out.println("  " + Theme.muted(hint));
        System.out.println();
    }

    /** Green confirmation line, e.g. after an entry is saved. */
    public static void ok(String message) {
        System.out.println("  " + Theme.ok("✓ " + message));
    }

    public static void warn(String message) {
        System.out.println("  " + Theme.warn("! " + message));
    }

    public static void blank() {
        System.out.println();
    }

    /** Greedy word wrap so long text stays inside the panel width. */
    public static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (current.length() > 0 && current.length() + 1 + word.length() > width) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    /** {@code key} left, {@code value} right, aligned inside the panel. */
    public static String row(String key, String value) {
        String padded = padRight(key, 10);
        return Theme.muted(padded) + "  " + Theme.strong(value);
    }

    private static String fit(String value, int width) {
        return value.length() <= width ? value : value.substring(0, width - 1) + "…";
    }

    private static String padRight(String value, int width) {
        return value + " ".repeat(Math.max(0, width - value.length()));
    }

    private static List<String> blockRows(String word) {
        List<String> rows = new ArrayList<>(WORDMARK_ROWS);
        for (int i = 0; i < WORDMARK_ROWS; i++) {
            rows.add("");
        }
        for (char character : word.toCharArray()) {
            List<String> letter = LETTERS.getOrDefault(character, List.of());
            int width = letter.stream().mapToInt(String::length).max().orElse(0);
            for (int i = 0; i < WORDMARK_ROWS; i++) {
                String line = i < letter.size() ? letter.get(i) : "";
                rows.set(i, rows.get(i) + padRight(line, width) + " ");
            }
        }
        return rows;
    }

    private static Map<Character, List<String>> letters() {
        Map<Character, List<String>> map = new LinkedHashMap<>();
        map.put('H', List.of(
                "██╗  ██╗",
                "██║  ██║",
                "███████║",
                "██╔══██║",
                "██║  ██║",
                "╚═╝  ╚═╝"));
        map.put('Y', List.of(
                "██╗   ██╗",
                "╚██╗ ██╔╝",
                " ╚████╔╝",
                "  ╚██╔╝",
                "   ██║",
                "   ╚═╝"));
        map.put('D', List.of(
                "██████╗",
                "██╔══██╗",
                "██║  ██║",
                "██║  ██║",
                "██████╔╝",
                "╚═════╝"));
        map.put('R', List.of(
                "██████╗",
                "██╔══██╗",
                "██████╔╝",
                "██╔══██╗",
                "██║  ██║",
                "╚═╝  ╚═╝"));
        map.put('O', List.of(
                " ██████╗",
                "██╔═══██╗",
                "██║   ██║",
                "██║   ██║",
                "╚██████╔╝",
                " ╚═════╝"));
        return map;
    }
}
