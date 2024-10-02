package io.github.spiaminto.dcscraper.dto;

import io.github.spiaminto.dcscraper.util.ContentCleaner;
import lombok.*;

import java.time.LocalDateTime;

@Builder @Getter @ToString
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
        return "DcComment(" +
                "id=" + id +
                ", commentNum=" + commentNum +
                ", boardNum=" + boardNum +
                ", writer=" + writer +
                ", cleanContent=" + getCleanContent() +
                ", regDate=" + regDate +
                ", reply=" + reply +
                ", targetNum=" + targetNum +
                ')';
    }
}
