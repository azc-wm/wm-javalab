package org.wm.scripting.test.threads.webcrawler;


import org.junit.jupiter.api.Test;
import org.wm.scripting.threads.webcrawler.WebScraper;

import java.net.URI;
import java.util.List;

public class WebScraperTesting {

    @Test
    public void readGoogle() throws Exception {
        try (WebScraper scraper = new WebScraper(0)) {

            scraper.enqueuUris(List.of(new URI("https://www.google.com")));
            scraper.run();

        }
    }
}
