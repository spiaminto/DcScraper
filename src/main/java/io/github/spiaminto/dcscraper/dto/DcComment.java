package io.github.spiaminto.dcscraper.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DcComment {

    private Long id;
    private Long commentNum; // dc 댓글 번호
    private Long boardNum; // dc 게시글 번호
    private String writer;
    private String content;
    private LocalDateTime regDate;

    private boolean reply; // 대댓글여부

    @Builder.Default
    private Long targetNum = -1L; // 대댓글 타겟 디시 댓글 번호

    public void setTargetNum(Long commentNum) {
        this.targetNum = commentNum;
    }

    public Long getId() {
        return id;
    }

    public Long getCommentNum() {
        return commentNum;
    }

    public Long getBoardNum() {
        return boardNum;
    }

    public String getWriter() {
        return writer;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getRegDate() {
        return regDate;
    }

    public boolean isReply() {
        return reply;
    }

    public Long getTargetNum() {
        return targetNum;
    }
}
