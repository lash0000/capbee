package com.hydronode.repository;

import com.hydronode.model.Sample;

import java.util.List;

/**
 * Storage seam. Today in-memory; a file or database implementation can be
 * dropped in without touching the service.
 */
public interface SampleRepository {

    void save(Sample sample);

    List<Sample> findAll();

    int count();
}
