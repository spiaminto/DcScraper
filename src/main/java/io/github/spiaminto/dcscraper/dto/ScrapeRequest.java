package io.github.spiaminto.dcscraper.dto;

public class ScrapeRequest {

    private String galleryId;
    private long startPage;
    private long endPage;
    private long interval;

    protected ScrapeRequest(long startPage, long endPage, long interval) {
        if (startPage < 0) {
            throw new IllegalArgumentException("startPage must not be less than zero");
        }

        if (endPage < 0 || endPage < startPage) {
            throw new IllegalArgumentException("endPage must not be less than zero or startpage");
        }

        if (interval < 0 || interval > endPage - startPage + 1) {
            throw new IllegalArgumentException("interval must not be less than zero or more than (endPage - startPage + 1)");
        }

        this.startPage = startPage;
        this.endPage = endPage;
        this.interval = interval;
    }

    public static ScrapeRequest of(long startPage, long endPage, long interval) {
        return new ScrapeRequest(startPage, endPage, interval);
    }

    public static ScrapeRequest of(long startPage, long endPage) {
        return new ScrapeRequest(startPage, endPage, 0);
    }

    public long getStartPage() {
        return startPage;
    }

    public long getEndPage() {
        return endPage;
    }

    public long getInterval() {
        return interval;
    }
}
