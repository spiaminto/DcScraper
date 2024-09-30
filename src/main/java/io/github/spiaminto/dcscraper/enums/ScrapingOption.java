package io.github.spiaminto.dcscraper.enums;

/**
 * 스크래핑 옵션 <br>
 * LISTPAGE : 리스트 페이지만 추출 <br>
 * VIEWPAGE : 리스트 페이지 + 상세 페이지에서 글 내용만 추출 <br>
 * ALL : 리스트 페이지 + 상세 페이지에서 글, 댓글 내용 모두 추출 (기본값)
 */
public enum ScrapingOption {
    /** 리스트 페이지만 추출 */
    LISTPAGE("listpage"),
    /** 리스트 페이지 + 상세 페이지에서 글 내용만 추출 */
    VIEWPAGE("viewpage"),
    /** 리스트 페이지 + 상세 페이지에서 글, 댓글 내용 모두 추출 */
    ALL("all");

    private final String value;

    ScrapingOption(String value) {
        this.value = value;
    }

}
