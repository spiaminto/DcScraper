package io.github.spiaminto.dcscraper.dto;

import io.github.spiaminto.dcscraper.util.ContentCleaner;
import lombok.*;

import java.time.LocalDateTime;

@Builder @Getter @ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DcBoard {

    private Long dcNum; // 글 번호
    private String title; // 제목
    private String writer; // 작성자 닉네임
    private String content; // 내용
    private LocalDateTime regDate; // 작성일

    private long viewCnt; // 조회수
    private long commentCnt; // 댓글수
    private long recommendCnt; // 추천수
    private boolean recommended; // 개념글 여부
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

