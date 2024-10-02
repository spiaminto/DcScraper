package io.github.spiaminto.dcscraper.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
public class ContentCleaner {
    private ContentCleaner() {}

    /**
     * HTML String (DcBoard.content) 을 받아서 제목 + 내용만 추출 using Jsoup
     * @param rawContent
     * @return cleanContent
     */
    public static String cleanContent(String rawContent) {
        if (rawContent == null) return "null"; // null 처리
        // 제목 +  내용만 추출
        Element rawElement = Jsoup.parse(rawContent);
        rawElement.select("#dcappfooter").remove(); // 앱푸터 삭제
        rawElement.select(".imgwrap").remove(); // 이미지 삭제
        rawElement.select(".lnk").remove(); // 링크삭제
        Element cleanedElement = Jsoup.parse(
                Jsoup.clean(rawElement.html(), Safelist.basic()) // 글 관련 태그 남기고 삭제
        );
        return cleanedElement.text(); // 내부문자 추출
    }

}
