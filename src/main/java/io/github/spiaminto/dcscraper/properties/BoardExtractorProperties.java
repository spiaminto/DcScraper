package io.github.spiaminto.dcscraper.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component @ConfigurationProperties(prefix = "board-extractor")
@Getter @Setter
public class BoardExtractorProperties {
    private final String boardNumSelector = ".gall_num";
    private final String boardNumAttr = "text";

    private final String titleSelector = ".gall_tit>a";
    private final String titleAttr = "text";

    private final String writerSelector = ".gall_writer>.nickname";
    private final String writerAttr = "title";

    private final String regDateSelector = ".gall_date";
    private final String regDateAttr = "title";

    private final String viewCntSelector = ".gall_count";
    private final String viewCntAttr = "text";

    private final String commentCntSelector = ".reply_num"; // attr 은 text 로 고정됨

    private final String recommendCntSelector = ".gall_recommend";
    private final String recommendCntAttr = "text";

    private final String contentSelector = ".write_div";
    private final String contentAttr = "innerHtml";

    private final String recommendSelector = ".icon_recomimg"; // 글 리스트에서 개념글 확인(아이콘)

    // private final String recommendSelector = ".btn_recommend_box .btn_recom_up.on"; // 글 내용에서 개념글 확인(추천버튼)
    // recommendAttr 은 .isEmpty 로 검사
}
