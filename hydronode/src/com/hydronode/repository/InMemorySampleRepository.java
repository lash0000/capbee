package com.hydronode.repository;

import com.hydronode.model.Sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Keeps entries for the lifetime of the process. */
public final class InMemorySampleRepository implements SampleRepository {

    private final List<Sample> samples = new ArrayList<>();

    @Override
    public void save(Sample sample) {
        samples.add(sample);
    }

    @Override
    public List<Sample> findAll() {
        return Collections.unmodifiableList(samples);
    }

    @Override
    public int count() {
        return samples.size();
    }
}
