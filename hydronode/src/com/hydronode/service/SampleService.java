package com.hydronode.service;

import com.hydronode.model.Sample;

import java.util.List;
import java.util.Optional;

/** Business rules for producing and keeping test entries. */
public interface SampleService {

    /** A random row taken from the real dataset, if the CSV is present. */
    Optional<Sample> randomReal();

    /** A plausible row built from the schema's own ranges and options. */
    Sample synthetic();

    /** Stores the entry, assigning a local id when it has none. */
    Sample save(Sample sample);

    List<Sample> history();

    int count();
}
