package com.hydronode.leak;

/**
 * One aligned sensor sample: what the network measured at a timestamp, plus the
 * ground-truth label from the leakages file.
 *
 * @param flow        inflow reading in L/s (sensor {@code p227})
 * @param pressure    mean of all pressure sensors, in metres of head
 * @param actualLeak  true when at least one pipe is labelled leaking
 */
public record LeakReading(String timestamp, double flow, double pressure, boolean actualLeak) {
}
