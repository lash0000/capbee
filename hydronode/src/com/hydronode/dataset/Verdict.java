package com.hydronode.dataset;

/**
 * The answer for one sample.
 *
 * @param label   short verdict, e.g. {@code LEAK} or {@code NO LEAK}
 * @param detail  one line of supporting context
 * @param alert   true when the verdict deserves a warning colour
 */
public record Verdict(String label, String detail, boolean alert) {
}
