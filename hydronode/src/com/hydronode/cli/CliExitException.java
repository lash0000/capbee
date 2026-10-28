package com.hydronode.cli;

/**
 * Thrown when standard input is closed (Ctrl-D). The menu loop unwinds instead
 * of looping forever on empty reads.
 */
public class CliExitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CliExitException() {
        super("standard input closed");
    }
}
