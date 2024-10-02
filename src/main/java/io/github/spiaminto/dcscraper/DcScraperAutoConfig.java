package io.github.spiaminto.dcscraper;

import io.github.spiaminto.dcscraper.extractor.BoardExtractor;
import io.github.spiaminto.dcscraper.extractor.CommentExtractor;
import io.github.spiaminto.dcscraper.properties.BoardExtractorProperties;
import io.github.spiaminto.dcscraper.properties.CommentExtractorProperties;
import io.github.spiaminto.dcscraper.properties.PageFinderProperties;
import io.github.spiaminto.dcscraper.properties.ScraperProperties;
import io.github.spiaminto.dcscraper.service.DcPageFinder;
import io.github.spiaminto.dcscraper.service.DcScraper;
import io.github.spiaminto.dcscraper.service.impl.DefaultDcPageFinder;
import io.github.spiaminto.dcscraper.service.impl.DefaultDcScraper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DcScraperAutoConfig {

    private final BoardExtractorProperties boardExtractorProperties;
    private final CommentExtractorProperties commentExtractorProperties;
    private final ScraperProperties scraperProperties;
    private final PageFinderProperties pageFinderProperties;

    @Bean
    public BoardExtractor boardExtractor() {
        return new BoardExtractor(boardExtractorProperties);
    }

    @Bean
    public CommentExtractor commentExtractor() {
        return new CommentExtractor(commentExtractorProperties);
    }

    @Bean
    @ConditionalOnMissingBean(DcScraper.class)
    public DcScraper dcScraper() {
        return new DefaultDcScraper(boardExtractor(), commentExtractor(), scraperProperties);
    }

    @Bean
    @ConditionalOnMissingBean(DcPageFinder.class)
    public DcPageFinder dcPageFinder() {
        return new DefaultDcPageFinder(boardExtractor(), pageFinderProperties);
    }

}
