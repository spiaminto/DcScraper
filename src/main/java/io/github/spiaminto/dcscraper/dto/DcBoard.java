package io.github.spiaminto.dcscraper.dto;

import io.github.spiaminto.dcscraper.util.ContentCleaner;
import lombok.*;

import java.time.LocalDateTime;

@Builder @Getter @ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DcBoard {

    private Long dcNum;
    private String title;
    private String writer;
    private String content;
    private LocalDateTime regDate;

    private long viewCnt;
    private long commentCnt;
    private long recommendCnt;
    private boolean recommended;
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * content 에서 html 태그와 불필요한 문자를 제거한 문자열을 반환합니다.
     * @return 처리된 content
     */
    public String getCleanContent() {
        return ContentCleaner.cleanContent(content);
    }

    /**
     * content 에서 html 태그와 불필요한 문자를 제거한 cleanContent 로 toString 합니다.
     * @return toString 결과
     */
    public String cleanedToString() {
        return "DcBoard(" +
                "dcNum=" + dcNum +
                ", title=" + title +
                ", cleanContent=" + getCleanContent() +
                ", writer=" + writer +
                ", regDate=" + regDate +
                ", viewCnt=" + viewCnt +
                ", commentCnt=" + commentCnt +
                ", recommendCnt=" + recommendCnt +
                ", recommended=" + recommended +
                ')';
    }
}

