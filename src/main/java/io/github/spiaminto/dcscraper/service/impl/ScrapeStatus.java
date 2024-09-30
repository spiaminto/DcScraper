package io.github.spiaminto.dcscraper.service.impl;

import io.github.spiaminto.dcscraper.dto.LoggingParams;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ScrapeStatus {
    private long startPage;
    private long endPage;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private long totalPageCnt;
    private long boardPerPage;
    private long totalBoardCnt;
    private long totalCommentCnt;
    private long totalDeletedCommentCnt;
    private long totalCommentCntFromBoard;
    private long expectedTotalBoardCnt;
    private long expectedTotalCommentCnt;

    public ScrapeStatus(long startPage, long endPage, LocalDateTime startTime) {
        this.startPage = startPage;
        this.endPage = endPage;
        this.startTime = startTime;
    }

    public void end() {
        this.endTime = LocalDateTime.now();
    }

    public long getExecutedPageCount() {
        return this.totalPageCnt;
    }

    public void syncScrapeStatus(LoggingParams loggingParams) {
        this.boardPerPage = loggingParams.getBoardPerPage();
        this.totalPageCnt += loggingParams.getScrapedPageCnt();
        this.totalBoardCnt += loggingParams.getScrapedBoardCount();
        this.totalCommentCnt += loggingParams.getScrapedCommentCnt();
        this.totalDeletedCommentCnt += loggingParams.getScrapedDeletedCommentCnt();
        this.totalCommentCntFromBoard += loggingParams.getScrapedBoardCommentCntTotal();
        this.expectedTotalBoardCnt += loggingParams.getExpectedBoardCnt();
        this.expectedTotalCommentCnt += loggingParams.getExpectedCommentCnt();
    }
}
