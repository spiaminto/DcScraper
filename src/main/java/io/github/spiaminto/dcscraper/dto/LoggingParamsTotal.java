package io.github.spiaminto.dcscraper.dto;

import lombok.Data;

@Data
public class LoggingParamsTotal {
    // 전체
    long totalPageCnt; // 전체 페이지 수
    long boardPerPage; // 페이지 당 게시글 수
    long totalBoardCnt; // 전체 게시글 수

    public long getExpectedTotalBoardCnt() {
        return totalPageCnt * boardPerPage; // 정상 실행시 예측되는 게시글 수
    }
    long totalCommentCnt;
    long totalDeletedCommentCnt;
    long totalCommentCntFromBoard;
    public long getExpectedTotalCommentCnt() {
        return totalCommentCntFromBoard + totalDeletedCommentCnt;
    }
}
