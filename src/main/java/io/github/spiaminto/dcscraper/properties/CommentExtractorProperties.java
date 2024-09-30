package io.github.spiaminto.dcscraper.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component @ConfigurationProperties(prefix = "comment-extractor")
@Getter @Setter
public class CommentExtractorProperties {

    private final String commentNumSelector = ".cmt_info";
    private final String commentNumAttr = "data-no";

    private final String replyNumSelector = ".reply_info";
    private final String replyNumAttr = "data-no";

    private final String commentWriterSelector = ".cmt_nickbox .gall_writer";
    private final String commentWriterAttr = "data-nick";

    private final String commentContentSelector = ".cmt_txtbox";
    private final String commentContentAttr = "innerHtml";

    private final String commentRegDateSelector = ".date_time";
    private final String commentRegDateAttr = "text";

    private final String isDorySelector = ".dory"; // 댓글돌이 검사용 선택자
    private final String isReplySelector = ".reply_list"; // 답글여부 검사용 선택자
    private final String isDeletedSelector = ".del_reply"; // 댓글/답글 삭제여부 검사용 선택자

    private final String replyListItemSelector = ".reply_list>li"; // 답글 리스트 아이템 (li) 선택자
    private final String isDeletedAttr = "text"; // 삭제된 댓글의 내용 추출용 attr
}
