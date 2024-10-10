package io.github.spiaminto.dcscraper.service.impl;

import io.github.spiaminto.dcscraper.TestConfiguration;
import io.github.spiaminto.dcscraper.dto.DcBoard;
import io.github.spiaminto.dcscraper.dto.DcBoardsAndComments;
import io.github.spiaminto.dcscraper.dto.DcComment;
import io.github.spiaminto.dcscraper.dto.ScrapeRequest;
import io.github.spiaminto.dcscraper.enums.GalleryType;
import io.github.spiaminto.dcscraper.service.DcScraper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    @Test
    public void callbackTest() {
        dcScraper.setCutCounter(3);
        dcScraper.startWithCallback(
                ScrapeRequest.of("granblue", GalleryType.MAJOR, 1, 2, 1),
                this::writeToFile);

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

    @Test
    public void writeToFile(DcBoardsAndComments scraped) {
        // 현재시간
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
        // 파일 생성
        File boardCsv = new File("c:\\scraper\\Board " + now + ".txt");
        File commentCsv = new File("c:\\scraper\\Comment " + now + ".txt");
        // 글 파일 작성

        try(FileWriter boardWriter = new FileWriter(boardCsv);
            FileWriter commentWriter = new FileWriter(commentCsv)) {

            List<DcBoard> boards = scraped.getBoards();
            boardWriter.write("번호,제목,내용,글쓴이,작성일,조회수,댓글수,추천수,개념글여부\n");
            for (DcBoard board : boards) {
                boardWriter.write(board.writeToString() + "\n");
            }
            commentWriter.write("댓글번호,글번호,글쓴이,내용,작성일,답글여부,답글대상 댓글번호\n");
            List<DcComment> comments = scraped.getComments();
            for (DcComment comment : comments) {
                commentWriter.write(comment.writeToString() + "\n");
            }
        } catch (IOException e) {
            System.out.println("IOException message = " + e.getMessage());
        }
    }


}
