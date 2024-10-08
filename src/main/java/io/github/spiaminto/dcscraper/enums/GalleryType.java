package io.github.spiaminto.dcscraper.enums;

/**
 * 갤러리 타입 <br>
 * MAJOR : 일반(정규) 갤러리 <br>
 * MINOR : 마이너 갤러리 <br>
 */
public enum GalleryType {
    MINOR("마이너 갤러리"),
    MAJOR("갤러리");

    private final String value;

    GalleryType(String value) {
        this.value = value;
    }
}
