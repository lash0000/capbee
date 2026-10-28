package com.hydronode.cli;

import com.hydronode.progress.ProgressListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The scanning screen: a spinner, a progress bar and a running row count,
 * redrawn in place inside a panel on the shared grid.
 *
 * <p>When output is not a terminal it degrades to a single summary line.</p>
 */
public final class ConsoleProgress implements ProgressListener {

    private static final long RENDER_INTERVAL_NANOS = 60_000_000L;
    private static final String[] SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final int BAR_WIDTH = 40;
    private static final int INNER = Frames.WIDTH - 2;

    private final boolean interactive = Terminal.isInteractive();

    private String source = "";
    private long total;
    private long startNanos;
    private long lastRenderNanos;
    private int renderedLines;
    private int frame;

    @Override
    public void start(String source, long total) {
        this.source = source;
        this.total = total;
        this.startNanos = System.nanoTime();
        this.lastRenderNanos = 0L;
        this.renderedLines = 0;
        this.frame = 0;

        if (interactive) {
            render(0L, false);
        }
    }

    @Override
    public void update(long units) {
        if (!interactive) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastRenderNanos < RENDER_INTERVAL_NANOS) {
            return;
        }
        lastRenderNanos = now;
        render(units, false);
    }

    @Override
    public void finish(long units) {
        double seconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;

        if (interactive) {
            render(units, true);
            return;
        }
        System.out.printf("  scanning %s: %,d rows in %.2fs%n", source, units, seconds);
    }

    private void render(long rows, boolean done) {
        List<String> lines = frame(rows, done);

        if (renderedLines > 0) {
            Terminal.moveUp(renderedLines);
        }
        for (String line : lines) {
            System.out.print("\r" + line);
            Terminal.eraseLine();
            System.out.print("\n");
        }
        renderedLines = lines.size();
        System.out.flush();
        frame++;
    }

    private List<String> frame(long rows, boolean done) {
        double seconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        double ratio = total > 0 ? Math.min(1.0, (double) rows / total) : 0.0;

        String spinner = done ? Theme.ok("✔") : Theme.accent(SPINNER[frame % SPINNER.length]);
        String status = done ? Theme.ok("done") : Theme.muted("scanning");

        List<String> lines = new ArrayList<>();
        lines.add(Frames.top("Scanning"));
        lines.add(Frames.body(spinner + " " + Theme.muted(shorten(source)) + "   " + status));
        lines.add(Frames.body(""));
        lines.add(Frames.body(bar(ratio) + "  " + Theme.strong(String.format(Locale.ROOT, "%5.1f%%", ratio * 100))));
        lines.add(Frames.body(Theme.strong(String.format(Locale.ROOT, "%,d", rows))
                + Theme.muted(String.format(Locale.ROOT, " / %,d rows  ·  %.2fs", total, seconds))));
        lines.add(Frames.bottom());
        return lines;
    }

    private static String bar(double ratio) {
        int filled = (int) Math.round(ratio * BAR_WIDTH);
        return Theme.accent("█".repeat(filled)) + Theme.muted("░".repeat(BAR_WIDTH - filled));
    }

    private static String shorten(String value) {
        return value.length() <= INNER - 20 ? value : "…" + value.substring(value.length() - (INNER - 22));
    }
}
