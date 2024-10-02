package io.github.spiaminto.dcscraper;

import io.github.spiaminto.dcscraper.dto.DcBoardsAndComments;
import io.github.spiaminto.dcscraper.dto.ScrapeRequest;
import io.github.spiaminto.dcscraper.service.DcScraper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

@SpringBootTest
@AutoConfigureMockMvc
public class DefaultDcScraperTest {
    public static Logger log = Logger.getLogger(DefaultDcScraperTest.class.getName());
    @Autowired
    DcScraper dcScraper;

    @Test
    public void scrapeTest() {
        dcScraper.setCutCounter(3);
        DcBoardsAndComments extracted = dcScraper.start(ScrapeRequest.of("github", true, 1, 1, 1));
        extracted.getBoards().forEach(dcBoard -> {
            log.info(dcBoard.cleanedToString());
        });
        extracted.getComments().forEach(dcComment -> {
            log.info(dcComment.cleanedToString());
        });
        dcScraper.startWithCallback(ScrapeRequest
                .of("github", true, 1, 1, 1), this::showResult);

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
