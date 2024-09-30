package io.github.spiaminto.dcscraper.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component @ConfigurationProperties(prefix = "scraper")
@Getter @Setter
public class ScraperProperties {
    // 스크래핑 할 url, 가변 uri
    private String baseUrl = "http://gall.dcinside.com";

    private String galleryListUri = "/board/lists/";
    private String minorGalleryListUri = "/mgallery/board/lists/";

    private String galleryViewUri = "/board/view/"; // 안씀

    private String galleryIdParameterPrefix = "?id="; // 반드시 첫번째로 사용하는 갤러리 이름 파라미터

    private String pageParameterPrefix = "&page="; // 페이징 파라미터

    private String listNumParameterPrefix = "&list_num="; // 한 페이지당 글 갯수
    private String listNum = "100"; // 한 페이지당 글 갯수 (30, 50, 100)

    private String searchParameterPrefix = "&s_type=search_subject_memo&s_keyword="; // 검색 파라미터(제목+내용)

    private String boardListSelector = ".gall_list";
    private String boardListItemSelector = "tbody tr";

    private String boardHrefSelector = ".gall_tit>a";
    private String boardViewSelector = ".gallery_view";
    private String boardViewSelectorAlter = "#container";

    private String boardViewContentSelector = ".write_div"; // 글 내용 로드 기다릴때 사용

    private String commentListSelector = ".cmt_list"; // 댓글 내용 로드 기다릴때 사용
    private String commentListItemSelector = ".cmt_list>li";
    
    private int maxRetryCount = 15;
}
