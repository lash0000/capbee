package com.hydronode.cli;

/**
 * Color palette. Every style is a no-op when the output is not a terminal or
 * when {@code NO_COLOR} is set, so piping to a file never leaks escape codes.
 */
public final class Theme {

    private static final String RESET = "\u001b[0m";
    private static final String BOLD = "\u001b[1m";
    private static final String DIM = "\u001b[2m";
    private static final String GREEN = "\u001b[32m";
    private static final String YELLOW = "\u001b[33m";
    private static final String CYAN = "\u001b[96m";

    private static final boolean ENABLED = Terminal.isInteractive()
            && System.getenv("NO_COLOR") == null;

    private Theme() {
    }

    public static String border(String text) {
        return paint(CYAN, text);
    }

    public static String title(String text) {
        return paint(BOLD + CYAN, text);
    }

    public static String accent(String text) {
        return paint(BOLD + CYAN, text);
    }

    public static String strong(String text) {
        return paint(BOLD, text);
    }

    public static String muted(String text) {
        return paint(DIM, text);
    }

    public static String ok(String text) {
        return paint(GREEN, text);
    }

    public static String warn(String text) {
        return paint(YELLOW, text);
    }

    private static String paint(String code, String text) {
        return ENABLED ? code + text + RESET : text;
    }
}
