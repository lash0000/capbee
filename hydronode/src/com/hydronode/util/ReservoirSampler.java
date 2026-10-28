package com.hydronode.util;

import java.util.Optional;
import java.util.Random;

/**
 * Reservoir sampling (Algorithm R) with a reservoir of size 1: keeps a single
 * uniformly random item from a stream of unknown length using constant memory.
 *
 * <p>Every incoming item has probability {@code 1/n} of replacing the current
 * pick, which makes the final choice uniformly distributed across the whole
 * stream — without storing it.</p>
 */
public final class ReservoirSampler<T> {

    private final Random random;

    private T selected;
    private long seen;

    public ReservoirSampler(Random random) {
        this.random = random;
    }

    public void offer(T item) {
        seen++;
        if (selected == null || random.nextLong(seen) == 0L) {
            selected = item;
        }
    }

    public Optional<T> result() {
        return Optional.ofNullable(selected);
    }

    public long seen() {
        return seen;
    }
}
