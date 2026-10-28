package com.hydronode;

import com.hydronode.dataset.Dataset;
import com.hydronode.service.SampleService;

/**
 * One selectable dataset together with the service stack that works on it, so
 * switching datasets is just switching index — no mutable shared state.
 */
public record Track(Dataset dataset, SampleService service) {
}
