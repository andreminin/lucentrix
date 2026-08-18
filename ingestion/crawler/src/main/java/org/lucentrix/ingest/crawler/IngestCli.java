package org.lucentrix.ingest.crawler;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class IngestCli implements ApplicationRunner {

    private final IngestLifecycle lifecycle;

    public IngestCli(IngestLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean ingest = args.getNonOptionArgs().contains("ingest") || args.containsOption("ingest");
        if (ingest) {
            boolean once = args.containsOption("once") || args.containsOption("no-ui") || args.containsOption("cli");
            lifecycle.start(once);
        }
    }
}
