package io.github.spiaminto.dcscraper.service.impl;

import io.github.spiaminto.dcscraper.TestConfiguration;
import io.github.spiaminto.dcscraper.dto.DcBoardsAndComments;
import io.github.spiaminto.dcscraper.dto.ScrapeRequest;
import io.github.spiaminto.dcscraper.enums.GalleryType;
import io.github.spiaminto.dcscraper.service.DcScraper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

@SpringBootTest(classes = TestConfiguration.class)
public class DefaultDcScraperTest {
    public static Logger log = Logger.getLogger(DefaultDcScraperTest.class.getName());
    @Autowired DcScraper dcScraper;

    @Test
    public void scrapeTest() {
        dcScraper.setCutCounter(3);
        DcBoardsAndComments extracted = dcScraper.start(ScrapeRequest.of("github", GalleryType.MINOR, 1, 3));
        extracted.getBoards().forEach(dcBoard -> {
            log.info(dcBoard.cleanedToString());
        });
        extracted.getComments().forEach(dcComment -> {
            log.info(dcComment.cleanedToString());
        });
        dcScraper.startWithCallback(ScrapeRequest
                .of("granblue", GalleryType.MAJOR, 1, 2, 1), this::showResult);

    }

    public void showResult(DcBoardsAndComments dcBoardsAndComments) {
        log.info("Total boards: " + dcBoardsAndComments.getBoards().size());
        log.info("Total comments: " + dcBoardsAndComments.getComments().size());
        dcBoardsAndComments.getBoards().forEach(dcBoard -> {
            log.info(dcBoard.cleanedToString());
        });
        dcBoardsAndComments.getComments().forEach(dcComment -> {
            log.info(dcComment.cleanedToString());
        });

    }


}
