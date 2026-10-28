package com.hydronode.leak;

/**
 * Normal operating point, taken from rows the dataset labels as leak-free.
 *
 * @param flow     nominal inflow {@code Q0}
 * @param pressure nominal pressure {@code P0}
 * @param samples  how many healthy rows the medians came from
 */
public record LeakBaseline(double flow, double pressure, int samples) {
}
