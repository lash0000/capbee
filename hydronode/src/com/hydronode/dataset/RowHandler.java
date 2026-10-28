package com.hydronode.dataset;

import java.util.List;

/** Receives one data row at a time while a dataset is streamed. */
@FunctionalInterface
public interface RowHandler {

    void accept(List<String> headers, long rowIndex, String[] row);
}
