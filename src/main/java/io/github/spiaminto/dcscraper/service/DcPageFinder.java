package io.github.spiaminto.dcscraper.service;

import java.time.LocalDate;

public interface DcPageFinder {

    /**
     * 주어진 날짜의 첫 글이 있는 페이지를 찾습니다.
     * @param inputDate 찾을 날짜
     * @param galleryId 갤러리 아이디
     * @param isMinorGallery 마이너 갤러리 여부
     */
    void findPage(LocalDate inputDate, String galleryId, boolean isMinorGallery);
}
