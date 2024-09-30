package io.github.spiaminto.dcscraper.dto;

import java.util.List;

/**
 * 게시글과 댓글을 담는 DTO<br>
 * List<DcBoard> boards : 게시글 리스트 <br>
 * List<DcComment> comments : 댓글 리스트 <br><br>
 * 스크래핑 할때 지정한 ScrapingOption 에 따라 결과값이 변화합니다 <br>
 * LISTPAGE : boards 채워짐 (board.content = null), comments 빈 리스트 <br>
 * VIEWPAGE : boards 채워짐 (board.content 포함), comments 빈 리스트 <br>
 * ALL : boards 채워짐 (board.content 포함), comments 채워짐 <br>
 * @see io.github.spiaminto.dcscraper.enums.ScrapingOption
 * @see DcBoard
 * @see DcComment
 *

 */
public class DcBoardsAndComments {

    private List<DcBoard> boards;
    private List<DcComment> comments;
//    private List<ScrapeFailure> failures;

    public DcBoardsAndComments(List<DcBoard> boards, List<DcComment> comments) {
        this.boards = boards;
        this.comments = comments;
//        this.failures = failures;
    }

    /**
     * 스크래핑 한 결과 중 게시글 목록을 가져옵니다.
     * SCRAPING_OPTION 이 LISTPAGE 일 경우 board.content 가 null 입니다.
     * @return 게시글 목록
     */
    public List<DcBoard> getBoards() {
        return boards;
    }

    /**
     * 스크래핑 한 결과 중 댓글 목록을 가져옵니다.
     * SCRAPING_OPTION 이 LISTPAGE 또는 VIEWPAGE 일 경우 댓글이 없습니다. comments.size() = 0
     * @return 댓글 목록
     */
    public List<DcComment> getComments() {
        return comments;
    }

}
