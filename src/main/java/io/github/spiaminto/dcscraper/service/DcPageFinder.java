package io.github.spiaminto.dcscraper.service;

import java.time.LocalDateTime;

public interface DcPageFinder {

    void findFirstPageByDate(LocalDateTime inputDateTime, String galleryId, boolean isMinorGallery);
}
