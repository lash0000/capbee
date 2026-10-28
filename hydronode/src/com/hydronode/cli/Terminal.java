package com.hydronode.cli;

/**
 * Small terminal helpers. Cursor control is a no-op when the program is not
 * attached to a real terminal, so piped output stays plain text.
 */
public final class Terminal {

    private static final String CURSOR_UP = "\u001b[%dA";
    private static final String ERASE_LINE = "\u001b[K";
    private static final String CLEAR_SCREEN = "\u001b[2J\u001b[H";

    private Terminal() {
    }

    public static boolean isInteractive() {
        return !isPlain() && System.console() != null;
    }

    /**
     * Safe mode for consoles that do not understand ANSI escapes (legacy
     * {@code cmd.exe}). Set {@code HYDRONODE_PLAIN=1} to disable colour, screen
     * clearing, in-place redraw and the row-by-row animation.
     */
    public static boolean isPlain() {
        return System.getenv("HYDRONODE_PLAIN") != null;
    }

    /** Clears the screen and homes the cursor. */
    public static void clearScreen() {
        if (isInteractive()) {
            System.out.print(CLEAR_SCREEN);
            System.out.flush();
        }
    }

    /** Moves the cursor up by {@code lines} without changing the column. */
    public static void moveUp(int lines) {
        System.out.print(CURSOR_UP.formatted(lines));
    }

    public static void eraseLine() {
        System.out.print(ERASE_LINE);
    }

    public static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
