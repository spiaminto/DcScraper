package io.github.spiaminto.dcscraper.service;

import io.github.spiaminto.dcscraper.dto.DcBoardsAndComments;
import io.github.spiaminto.dcscraper.dto.ScrapeRequest;
import io.github.spiaminto.dcscraper.enums.ScrapingOption;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface DcScraper {

    /**
     * 기존 WebDriver 를 종료하고 사용자 정의 WebDriver 를 등록합니다. WebDriver 와 WebDriverWait 는 삭제됩니다.<br>
     * null 입력시 WebDriver 와 WebDriverWait (2000ms) 는 기본값으로 초기화 됩니다.
     * @param webDriver
     * @see io.github.spiaminto.dcscraper.util.WebDriverUtil
     */
    void setWebDriver(WebDriver webDriver);

    /**
     * 기존 WebDriver 를 종료하고 사용자 정의 WebDriver 와 WebDriverWait 을 등록합니다.<br>
     * WebDriverWait 에 null 을 입력하면 기본값으로 초기화 됩니다. (2000ms)
     * @param webDriver
     * @param webDriverWait
     */
    void setWebDriver(WebDriver webDriver, WebDriverWait webDriverWait);

    /**
     * 사용자 정의 Explicit WebDriverWait 을 등록 합니다.
     * null 입력시 기본값 (2000ms) 으로 초기화 됩니다.
     * @param webDriverWait
     */
    void setWebDriverWait(WebDriverWait webDriverWait);

    /**
     * WebDriverWait 의 timeout 을 설정합니다.
     * @param timeout
     */
    void setWebDriverWaitTimeout(Duration timeout);

    /**
     * WebDriverWait 를 해제합니다.
     * WebDriver 기본값은 PageLoadStrategy = NONE 으로 설정되어 있습니다.
     */
    void clearWebDriverWait();

    /**
     * 사용자 정의 Executor 를 등록합니다. (callback 에 사용)
     * null 입력시 ForkJoinPool.commonPool() 로 초기화 됩니다.
     * @param executor
     */
    void setExecutor(Executor executor);

    /**
     * 스크래핑 중 페이지 이동간 대기시간을 설정합니다. 기본값 500ms
     * @param duration
     */
    void setIntervalTime(Duration duration);

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
     * 현재 WebDriver 를 종료합니다.
     */
    void quitDriver();

    /**
     * 스크래핑이 종료되면 WebDriver 를 자동으로 종료 되도록 설정합니다.<br>
     * 해당 설정이 true 면 스크래핑 종료시 자동으로 드라이버가 종료되며,
     * 사용자 지정 드라이버 사용 시 스크래핑 재시작 전에 해당 드라이버를 다시 setDriver 로 설정해야합니다.<br>
     * 기본 WebDriver 는 스크래핑 시작시 자동으로 재시작 합니다.<br>
     * 기본값 false
     */
    void setAutoQuitWebDriver(boolean autoQuitWebDriver);

    /**
     * 스크래핑 재시도 횟수를 설정합니다. 기본값 15
     * @param retryCount
     */
    void setMaxRetryCount(int retryCount);

}
