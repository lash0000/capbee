package com.hydronode.cli;

import java.util.List;

/**
 * Everything the screens need from the user. Implementing this interface
 * against something else (a script, a test double) keeps the menu logic
 * testable without a real terminal.
 */
public interface Prompt {

    void print(String message);

    void blank();

    /**
     * @return a value inside {@code [min, max]}
     */
    double askDouble(String label, double min, double max);

    /**
     * Numbered list with a header, for field choices.
     *
     * @return the 0-based index of the chosen option
     */
    int askChoice(String title, List<String> options);

    /**
     * Bare number prompt for menus; options are already on screen.
     *
     * @return the number the user typed, between {@code min} and {@code max}
     */
    int askNumber(String label, int min, int max);

    /** Free-text prompt for values that cannot be listed. */
    String askText(String label);

    /** Blocks until the user presses Enter. */
    void waitForEnter(String hint);
}
