package com.hydronode.service;

import com.hydronode.dataset.Dataset;
import com.hydronode.model.FieldValue;
import com.hydronode.model.Sample;
import com.hydronode.progress.ProgressListener;
import com.hydronode.repository.SampleRepository;
import com.hydronode.schema.FieldSpec;
import com.hydronode.util.ReservoirSampler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

/**
 * Default rules for test entries. Synthetic values are drawn from the ranges
 * and options the dataset itself reports, so nothing here is invented.
 *
 * <p>Real rows are picked by streaming the whole CSV once and keeping a single
 * uniformly random row (reservoir sampling), so memory stays flat regardless of
 * how big the file is.</p>
 */
public final class DefaultSampleService implements SampleService {

    private final Dataset dataset;
    private final SampleRepository repository;
    private final ProgressListener progress;
    private final Random random = new Random();

    private int sequence;

    public DefaultSampleService(Dataset dataset, SampleRepository repository, ProgressListener progress) {
        this.dataset = dataset;
        this.repository = repository;
        this.progress = progress;
    }

    @Override
    public Optional<Sample> randomReal() {
        if (!dataset.isAvailable()) {
            return Optional.empty();
        }

        List<String> headers = dataset.columns();
        ReservoirSampler<String[]> sampler = new ReservoirSampler<>(random);

        progress.start(dataset.sourceName(), dataset.rowCount());
        long total = dataset.streamRows((columns, index, row) -> {
            sampler.offer(row);
            progress.update(index + 1);
        });
        progress.finish(total);

        return sampler.result().map(row -> Sample.fromCsvRow(headers, row));
    }

    @Override
    public Sample synthetic() {
        List<FieldValue> values = new ArrayList<>();
        for (FieldSpec spec : dataset.schema().fields()) {
            values.add(new FieldValue(spec.name(), syntheticValue(spec)));
        }
        return new Sample(values);
    }

    private String syntheticValue(FieldSpec spec) {
        if (spec.isNumber()) {
            double value = spec.min() + random.nextDouble() * (spec.max() - spec.min());
            return String.format(Locale.ROOT, "%.2f", value);
        }
        List<String> options = spec.options();
        if (options.isEmpty()) {
            return "";
        }
        return options.get(random.nextInt(options.size()));
    }

    @Override
    public Sample save(Sample sample) {
        String id = sample.value("id");
        boolean missingId = id == null || id.isBlank();
        Sample stored = missingId && hasIdColumn()
                ? sample.withValue("id", nextId())
                : sample;
        repository.save(stored);
        return stored;
    }

    /** Only datasets that actually have an {@code id} column get local ids. */
    private boolean hasIdColumn() {
        return dataset.columns().stream().anyMatch(column -> column.equalsIgnoreCase("id"));
    }

    @Override
    public List<Sample> history() {
        return repository.findAll();
    }

    @Override
    public int count() {
        return repository.count();
    }

    private String nextId() {
        sequence++;
        return String.format(Locale.ROOT, "HN-%04d", sequence);
    }
}
