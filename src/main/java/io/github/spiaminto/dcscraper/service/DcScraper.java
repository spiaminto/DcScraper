package io.github.spiaminto.dcscraper.service;

import io.github.spiaminto.dcscraper.dto.DcBoardsAndComments;
import io.github.spiaminto.dcscraper.dto.ScrapeRequest;
import io.github.spiaminto.dcscraper.enums.ScrapingOption;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface DcScraper {

    /**
     * 사용자 정의 Executor 를 등록합니다. (callback 에 사용)
     * null 입력시 ForkJoinPool.commonPool() 로 초기화 됩니다.
     * @param executor
     */
    void setExecutor(Executor executor);

    /**
     * 스크래핑 옵션을 설정합니다.
     * @see ScrapingOption
     * @param scrapingOption
     */
    void setScrapingOption(ScrapingOption scrapingOption);

    /**
     * 스크래핑을 시작합니다.
     * @param scrapeRequest
     */
    DcBoardsAndComments start(ScrapeRequest scrapeRequest);

    /**
     * 주어진 콜백함수를 비동기로 등록하여 스크래핑을 시작합니다.
     * interval 이 2 일경우 두 페이지 마다 실행 (1-2-callback-3-4-callback...)
     *
     * @param scrapeRequest
     * @param callback      스크래핑 결과로 실행할 Consumer&lt;DcBoardsAndComments&gt; 콜백함수.
     */
    void startWithCallback(ScrapeRequest scrapeRequest, Consumer<DcBoardsAndComments> callback);

    /**
     * 스크래핑 재시도 횟수를 설정합니다. 기본값 15
     * @param retryCount
     */
    void setMaxRetryCount(int retryCount);

    /**
     * 개발용 컷카운터 설정, ScrapingOption.ALL 일때만 작동.
     * 기본값 0, 0보다 큰 값일 경우 컷 동작
     * @param cutCounter
     */
    void setCutCounter(long cutCounter);

}
