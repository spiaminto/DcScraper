package io.github.spiaminto.dcscraper.dto;

import io.github.spiaminto.dcscraper.util.ContentCleaner;
import lombok.*;

import java.time.LocalDateTime;

@Builder @Getter @ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DcComment {

    private Long commentNum; // 댓글 번호
    private Long boardNum; // 글 번호
    private String writer; // 작성자 닉네임
    private String content; // 내용
    private LocalDateTime regDate; // 작성일

    private boolean reply; // 답글(대댓글)여부

    @Builder.Default
    private Long targetNum = -1L; // 답글(대댓글) 타겟의 댓글 번호

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
                "commentNum=" + commentNum +
                ", boardNum=" + boardNum +
                ", writer=" + writer +
                ", cleanContent=" + getCleanContent() +
                ", regDate=" + regDate +
                ", reply=" + reply +
                ", targetNum=" + targetNum +
                ')';
    }

    /**
     * 메모장 또는 엑셀 파일로 저장하기 위한 형식으로 변환합니다. (댓글번호,글번호,글쓴이,내용,작성일,답글여부,답글대상 댓글번호)<br>
     * 구분자는 \t (탭) 입니다.
     * @return
     */
    public String writeToString() {
        return commentNum + "\t" +
                boardNum + "\t" +
                writer + "\t" +
                getCleanContent() + "\t" +
                regDate + "\t" +
                reply + "\t" +
                targetNum;
    }
}
