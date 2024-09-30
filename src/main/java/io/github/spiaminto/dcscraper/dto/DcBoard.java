package io.github.spiaminto.dcscraper.dto;

import lombok.*;

import java.time.LocalDateTime;

@Builder @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DcBoard {

    private Long dcNum;
    private String title;
    private String content;
    private String writer;
    private LocalDateTime regDate;

    private long viewCnt;
    private long commentCnt;
    private long recommendCnt;
    private boolean recommended;

    public void setTitle(String title) {
        this.title = title;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }

}

