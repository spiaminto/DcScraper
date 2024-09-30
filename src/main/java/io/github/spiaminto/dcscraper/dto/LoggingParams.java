package io.github.spiaminto.dcscraper.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoggingParams {

    long scrapedPageCnt; // 스크래핑된 페이지 수
    long boardPerPage; // 페이지 당 게시글 수

    long scrapedBoardCount; // 스크래핑된 게시글 수
    public long getExpectedBoardCnt() {
        return scrapedPageCnt * boardPerPage; // 정상실행시 예측되는 게시글 수
    }

    long scrapedCommentCnt; // 스크래핑 된 댓글 수
    long scrapedDeletedCommentCnt; // 스크래핑된 삭제된 댓글 수
    long scrapedBoardCommentCntTotal; //  board.getCommentCnt() 합
    public long getExpectedCommentCnt() {
        return scrapedBoardCommentCntTotal + scrapedDeletedCommentCnt; // 정상 실행시 예측되는 댓글 수
    }


}
