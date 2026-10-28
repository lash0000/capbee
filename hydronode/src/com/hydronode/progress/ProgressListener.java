package com.hydronode.progress;

/**
 * Receives progress from long-running work. The implementation decides how to
 * show it (animated panel, log line, nothing at all), which keeps the service
 * layer free of any output code.
 */
public interface ProgressListener {

    /**
     * @param source short description of what is being processed
     * @param total  expected number of units, or a negative value if unknown
     */
    void start(String source, long total);

    void update(long units);

    void finish(long units);

    /** Used when the caller does not care about progress. */
    static ProgressListener none() {
        return new ProgressListener() {
            @Override
            public void start(String source, long total) {
            }

            @Override
            public void update(long units) {
            }

            @Override
            public void finish(long units) {
            }
        };
    }
}
