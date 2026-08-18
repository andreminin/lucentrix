package org.lucentrix.ingest.crawler;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ingest")
public class IngestStatusController {

    private final IngestLifecycle lifecycle;

    public IngestStatusController(IngestLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return lifecycle.status();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        SseEmitter emitter = new SseEmitter(0L);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("progress").data(lifecycle.status()));
            } catch (IOException ex) {
                emitter.complete();
                scheduler.shutdownNow();
            }
        }, 0, 1, TimeUnit.SECONDS);
        emitter.onCompletion(scheduler::shutdownNow);
        emitter.onTimeout(scheduler::shutdownNow);
        return emitter;
    }

    @PostMapping("/start")
    public Map<String, Object> start() {
        lifecycle.start(false);
        return lifecycle.status();
    }

    @PostMapping("/stop")
    public Map<String, Object> stop() {
        lifecycle.stop();
        return lifecycle.status();
    }
}
