package org.wm.scripting.threads.webcrawler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class WebScraper implements Runnable, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(WebScraper.class.getName());

    private final int parallelism;

    private final BlockingQueue<URI> frontier = new LinkedBlockingQueue<>(10_000);
    private final Set<URI> visited = ConcurrentHashMap.newKeySet();
    // We do not need to use synchronized to interact with this volatile, because we have one writer and many readers
    private volatile boolean running = false;

    private final ExecutorService workers;
    private final HttpClient httpClient;

    public WebScraper(int parallelism) {
        this.parallelism = parallelism;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.workers = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void enqueuUris(List<URI> uris) {
        frontier.addAll(uris);
    }

    @Override
    public void run() {
        running = true;
        // Always run at least one worker, even if parallelism == 0
        int safeParallelism = this.parallelism;
        do {
            workers.submit(this::process);
            safeParallelism--;
        } while (safeParallelism > 0);

    }

    private void process() {
        while (running) {
            try {
                // If we wanted dynamic queue state, like having urls added during runtime, we coul use
                // frontier.take(); wich would leave the thread sleeping until there were available items
                URI uri = frontier.poll(1, TimeUnit.SECONDS);

                if (uri == null) {
                    // there is no work
                    if (!running && frontier.isEmpty()) break;
                    continue;
                }
                
                processUrl(uri);
                
            } catch (Exception e) {
                switch(e) {
                    case InterruptedException _ -> Thread.currentThread().interrupt();
                    case IOException _ -> System.out.println("IOException happened");
                    default -> throw new IllegalStateException("Unexpected value: " + e);
                }

                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private void processUrl(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .build();

        HttpResponse<?> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        LOGGER.info(String.format("Response received %s", response));
    }

    public void stop() {
        running = false;
    }

    @Override
    public void close() throws Exception {
            workers.shutdown();
            httpClient.close();
    }
}
