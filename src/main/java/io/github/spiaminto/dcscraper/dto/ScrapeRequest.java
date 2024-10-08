package io.github.spiaminto.dcscraper.dto;

import io.github.spiaminto.dcscraper.enums.GalleryType;
import lombok.Getter;

@Getter
public class ScrapeRequest {

    private final long startPage;
    private final long endPage;
    private final long interval;
    private final String galleryId;
    private final GalleryType galleryType;

    /**
     * 스크래핑 요청 생성
     * @see GalleryType
     * @param galleryId : 갤러리 id (주소창의 lists/?id= 뒤에 있는 값)
     * @param galleryType : 갤러리 타입
     * @param startPage : 시작 페이지
     * @param endPage : 끝 페이지
     * @return
     */
    public static ScrapeRequest of(String galleryId, GalleryType galleryType, long startPage, long endPage) {
        return new ScrapeRequest(galleryId, galleryType, startPage, endPage, 0);
    }

    /**
     * 콜백 스크래핑 요청 생성
     * @see GalleryType
     * @param galleryId : 갤러리 id (주소창의 lists/?id= 뒤에 있는 값)
     * @param galleryType : 갤러리 타입
     * @param startPage : 시작 페이지
     * @param endPage : 끝 페이지
     * @param interval : callback 을 실행할 간격
     * @return
     */
    public static ScrapeRequest of(String galleryId, GalleryType galleryType, long startPage, long endPage, long interval) {
        return new ScrapeRequest(galleryId, galleryType, startPage, endPage, interval);
    }

    protected ScrapeRequest(String galleryId, GalleryType galleryType, long startPage, long endPage, long interval) {
        if (galleryId == null || galleryId.isEmpty()) {
            throw new IllegalArgumentException("galleryId must not be null or empty");
        }
        if (startPage < 0) {
            throw new IllegalArgumentException("startPage must not be less than zero");
        }
        if (endPage < 0 || endPage < startPage) {
            throw new IllegalArgumentException("endPage must not be less than zero or startpage");
        }
        if (interval < 0 || interval > endPage - startPage + 1) {
            throw new IllegalArgumentException("interval must not be less than zero or more than (endPage - startPage + 1)");
        }
        if (galleryType == null) {
            throw new IllegalArgumentException("galleryType must not be null");
        }
        this.galleryId = galleryId;
        this.startPage = startPage;
        this.endPage = endPage;
        this.interval = interval;
        this.galleryType = galleryType;
    }
}
