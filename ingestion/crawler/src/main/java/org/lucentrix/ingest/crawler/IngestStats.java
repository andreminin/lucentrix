package org.lucentrix.ingest.crawler;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;
import lombok.Getter;

import java.time.Instant;

@Getter
public class IngestStats {
    String id;

    private final Gauge speedDocPerHour;
    private final Gauge runTimeMsec;
    private final Counter docCounter;
    private final Instant startTime;

    public IngestStats(String id, MeterRegistry registry) {
        this.id = id;
        this.startTime = Instant.now();

        String[] performanceTag = new String[] {"crawler", id};

        String prefix = "";

        this.runTimeMsec = Gauge.builder(prefix + "run.time", this, IngestStats::getRunTimeMsec)
                .description("run time")
                .baseUnit(BaseUnits.MILLISECONDS)
                .tags(performanceTag)
                .register(registry);

        this.speedDocPerHour = Gauge.builder(prefix + "docs.hr", this, IngestStats::getDocPerHour)
                .description("run time")
                .baseUnit(BaseUnits.OPERATIONS)
                .tags(performanceTag)
                .register(registry);

        this.docCounter = Counter
                .builder(prefix + "document.total")
                .description("count of crawled documents")
                .baseUnit(BaseUnits.OBJECTS)
                .tags(performanceTag)
                .register(registry);
    }

    public long getRunTimeMsec() {
        return Instant.now().toEpochMilli() - startTime.toEpochMilli();
    }

    public double getDocPerHour() {
        return  (3600000 * docCounter.count()) / getRunTimeMsec();
    }
}
