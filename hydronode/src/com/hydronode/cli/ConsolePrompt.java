package com.hydronode.cli;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * {@link Prompt} backed by {@link Scanner}. All validation and all prompt
 * styling lives here so the rest of the program can assume clean values and
 * never touches the keyboard directly.
 */
public final class ConsolePrompt implements Prompt {

    private static final String CURSOR = "  " + Theme.accent("❯") + " ";

    private final Scanner scanner;

    public ConsolePrompt() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void print(String message) {
        System.out.println(message);
    }

    @Override
    public void blank() {
        System.out.println();
    }

    @Override
    public double askDouble(String label, double min, double max) {
        while (true) {
            System.out.print(CURSOR + Theme.muted(label) + " "
                    + Theme.muted("[" + trim(min) + ".." + trim(max) + "]") + " ");
            System.out.flush();

            String line = readLine().trim().replace(',', '.');
            if (line.isEmpty()) {
                Frames.warn("value required");
                continue;
            }
            try {
                double value = Double.parseDouble(line);
                if (value < min || value > max) {
                    Frames.warn("must be between " + trim(min) + " and " + trim(max));
                    continue;
                }
                return value;
            } catch (NumberFormatException ex) {
                Frames.warn("not a number");
            }
        }
    }

    @Override
    public int askChoice(String title, List<String> options) {
        System.out.println("  " + Theme.strong(title));
        for (int i = 0; i < options.size(); i++) {
            System.out.println("   " + Theme.accent(String.format(Locale.ROOT, "%2d", i + 1))
                    + "  " + options.get(i));
        }
        return askNumber("choose", 1, options.size()) - 1;
    }

    @Override
    public int askNumber(String label, int min, int max) {
        while (true) {
            System.out.print(CURSOR + Theme.muted(label) + " ");
            System.out.flush();

            String line = readLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the hint below
            }
            Frames.warn("enter a number from " + min + " to " + max);
        }
    }

    @Override
    public void waitForEnter(String hint) {
        System.out.print("  " + Theme.muted(hint) + " ");
        System.out.flush();
        readLine();
    }

    @Override
    public String askText(String label) {
        while (true) {
            System.out.print(CURSOR + Theme.muted(label) + " ");
            System.out.flush();

            String line = readLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            Frames.warn("value required");
        }
    }

    private String readLine() {
        if (!scanner.hasNextLine()) {
            throw new CliExitException();
        }
        return scanner.nextLine();
    }

    private static String trim(double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.valueOf(value);
    }
}
