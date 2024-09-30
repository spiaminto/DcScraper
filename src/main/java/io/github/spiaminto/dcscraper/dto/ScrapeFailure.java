package io.github.spiaminto.dcscraper.dto;

import lombok.Data;

@Data
public class ScrapeFailure {

    long pageNum;
    long loadedSize; // 로드된 trElement 수
    long extractedSize; // extract 된 board 수
    String executeUrl;

    public ScrapeFailure(String executeUrl, long loadedSize, long extractedSize, long pageNum) {
        this.pageNum = pageNum;
        this.loadedSize = loadedSize;
        this.extractedSize = extractedSize;
        this.executeUrl = executeUrl;
    }

    // 1. 디시 제휴 글
    // 예시) 그랑블루 갤러리의 닉네임 '게임메카' 게시글은 제휴글로
    // class 의 us-post 클래스가 없어 minorGalleryExtractable 에서 걸러짐
    // 이로인해 trElement 가 101개로 모두 로드 된 상태에서 공지와 제휴글이 제외되어 99개 저장되는 경우 있음

}
