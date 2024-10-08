package io.github.spiaminto.dcscraper.service;

import io.github.spiaminto.dcscraper.enums.GalleryType;

import java.time.LocalDate;

public interface DcPageFinder {

    /**
     * 주어진 날짜의 첫 글이 있는 페이지를 찾습니다.
     * @see GalleryType
     * @param inputDate 찾을 날짜
     * @param galleryId 갤러리 아이디
     * @param galleryType 갤러리타입
     */
    void findPage(LocalDate inputDate, String galleryId, GalleryType galleryType);
}
