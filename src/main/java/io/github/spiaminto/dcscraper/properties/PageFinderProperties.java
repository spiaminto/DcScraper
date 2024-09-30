package io.github.spiaminto.dcscraper.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component @ConfigurationProperties(prefix = "page-finder")
@Getter @Setter
public class PageFinderProperties {

    // 스크래핑 할 url, 가변 uri
    private String baseUrl = "http://gall.dcinside.com";

    private String galleryListUri = "/board/lists/";
    private String minorGalleryListUri = "/mgallery/board/lists/";

    private String galleryViewUri = "/board/view/"; // 안씀

    private String galleryNameParameterPrefix = "?id="; // 반드시 첫번째로 사용하는 갤러리 이름 파라미터

    private String pageParameter = "&page="; // 페이징 파라미터

    private String searchParameter = "&s_type=search_subject_memo&s_keyword="; // 검색 파라미터(제목+내용)
}
