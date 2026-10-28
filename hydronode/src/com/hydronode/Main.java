package com.hydronode;

import com.hydronode.cli.CliExitException;
import com.hydronode.cli.ConsoleProgress;
import com.hydronode.cli.ConsolePrompt;
import com.hydronode.cli.Prompt;
import com.hydronode.dataset.Dataset;
import com.hydronode.dataset.IrrigationDataset;
import com.hydronode.dataset.LeakDataset;
import com.hydronode.dataset.PotabilityDataset;
import com.hydronode.progress.ProgressListener;
import com.hydronode.repository.InMemorySampleRepository;
import com.hydronode.service.DefaultSampleService;
import com.hydronode.service.SampleService;

import java.util.List;

/**
 * Entry point. Wires the object graph together and nothing else: every
 * behaviour lives in a collaborator that can be replaced (prompt, dataset,
 * repository, service).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Prompt prompt = new ConsolePrompt();
        ProgressListener progress = new ConsoleProgress();

        List<Track> tracks = List.of(
                track(new LeakDataset(), progress),
                track(new PotabilityDataset(), progress),
                track(new IrrigationDataset(), progress));

        try {
            new Application(prompt, tracks, progress).run();
        } catch (CliExitException exit) {
            // Ctrl-D / closed stdin: leave the loop without a stack trace.
            System.out.println();
            System.out.println("Bye.");
        }
    }

    /** Each dataset gets its own repository, so histories never mix. */
    private static Track track(Dataset dataset, ProgressListener progress) {
        SampleService service = new DefaultSampleService(
                dataset, new InMemorySampleRepository(), progress);
        return new Track(dataset, service);
    }
}
