package io.github.spiaminto.dcscraper.service.impl;


import com.google.common.base.Stopwatch;
import io.github.spiaminto.dcscraper.dto.*;
import io.github.spiaminto.dcscraper.enums.ScrapingOption;
import io.github.spiaminto.dcscraper.exception.RetryExceededException;
import io.github.spiaminto.dcscraper.extractor.BoardExtractor;
import io.github.spiaminto.dcscraper.extractor.CommentExtractor;
import io.github.spiaminto.dcscraper.properties.ScraperProperties;
import io.github.spiaminto.dcscraper.service.DcScraper;
import io.github.spiaminto.dcscraper.util.ContentCleaner;
import io.github.spiaminto.dcscraper.util.WebDriverUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class DefaultDcScraper implements DcScraper {
    private final CommentExtractor commentExtractor;
    private final BoardExtractor boardExtractor;
    private final ScraperProperties props;

    private WebDriver webDriver;
    private WebDriverWait webDriverWait;
    private long intervalTime; // millis
    private Executor executor;

    // 런타임 옵션
    private ScrapingOption scrapingOption;
    private String galleryId;
    private boolean autoQuitWebDriver;
    private int maxRetryCount;

    // 프로퍼티
    private String baseUrl = "http://gall.dcinside.com";
    private String galleryListUri, minorGalleryListUri;
    private String galleryIdParameterPrefix;
    private String pageParameterPrefix;
    private String listNumParameterPrefix, listNum;
    private String searchParameterPrefix;
    private String boardListSelector, boardListItemSelector;
    private String boardHrefSelector;
    private String boardViewSelector, boardViewSelectorAlter;
    private String boardViewContentSelector;
    private String commentListSelector, commentListItemSelector;

    @PostConstruct
    protected void setProperties() {
        this.galleryListUri = props.getGalleryListUri();
        this.minorGalleryListUri = props.getMinorGalleryListUri();

        this.galleryIdParameterPrefix = props.getGalleryIdParameterPrefix();

        this.pageParameterPrefix = props.getPageParameterPrefix();

        this.listNumParameterPrefix = props.getListNumParameterPrefix();
        this.listNum = props.getListNum();

        this.searchParameterPrefix = props.getSearchParameterPrefix();

        this.boardListSelector = props.getBoardListSelector();
        this.boardListItemSelector = props.getBoardListItemSelector();

        this.boardHrefSelector = props.getBoardHrefSelector();

        this.boardViewSelector = props.getBoardViewSelector();
        this.boardViewSelectorAlter = props.getBoardViewSelectorAlter();

        this.boardViewContentSelector = props.getBoardViewContentSelector();

        this.commentListSelector = props.getCommentListSelector();
        this.commentListItemSelector = props.getCommentListItemSelector();
        
    }

    public DefaultDcScraper(BoardExtractor boardExtractor, CommentExtractor commentExtractor, ScraperProperties scraperProperties) {
        this.boardExtractor = boardExtractor;
        this.commentExtractor = commentExtractor;
        this.props = scraperProperties;

        intervalTime = Duration.ofMillis(1000).toMillis(); // 기본값

        executor = ForkJoinPool.commonPool(); // default CompletableFuture executor

        scrapingOption = ScrapingOption.ALL;
        autoQuitWebDriver = false;
        
        // 드라이버 초기화는 생성자가 아닌, 별도의 메서드를 이용한다.
        // 빈이 등록되는 시점이 아니라 빈을 사용하는 시점에 드라이버를 초기화 해야 
        // 어플리케이션이 켜지자마자 쓸데없이 브라우저가 바로 켜짐을 방지가능
    }


    public void setWebDriver(WebDriver webDriver) {
        setDriver(webDriver, null);
    }

    public void setWebDriver(WebDriver webDriver, WebDriverWait webDriverWait) {
        setDriver(webDriver, webDriverWait);
    }

    protected void setDriver(WebDriver webDriver, WebDriverWait webDriverWait) {
        quitDriver();
        if (webDriver == null && webDriverWait == null) { // 기본 드라이버, 타이머로 초기화
            this.webDriver = WebDriverUtil.getChromeDriver();
            setWebDriverWait(null);
        } else if (webDriver != null && webDriverWait == null){ // 입력 드라이버 set, 타이머 삭제
            this.webDriver = webDriver;
            clearWebDriverWait();
        } else if (webDriver != null && webDriverWait != null) { // 입력 드라이버, 타이머 set
            this.webDriver = webDriver;
            setWebDriverWait(webDriverWait);
        } else {
            throw new IllegalArgumentException("잘못된 값 webDriver = " + webDriver + ", webDriverWait = " + webDriverWait);
        }
    }

    public void quitDriver() {
        if (isDriverAlive()) {
            webDriver.close();
            webDriver.quit();
            webDriverWait = null;
        }
    }

    /**
     * 현재 WebDriver 가 살아있는지 (not closed or quitted) 확인
     * @return NoSuchSessionException 발생시 false, 미발생시 true 리턴
     */
    protected boolean isDriverAlive() {
        if (webDriver == null) { return false; } 
        try {
            // 브라우저가 closed 또는 quit 되어도 webDriver == null 이 안됨. 따라서 별도 검증필요
            webDriver.getWindowHandle();
            return true; // 브라우저 살아있음
        } catch (NoSuchSessionException e) {
            return false; // 브라우저 닫힘
        } catch (Exception e) {
            log.error("WebDriver 상태 오류, webDriver = {}, \ne = {}",webDriver, e);
            throw e; // 알 수 없는 상태
        }
    }

    public void setIntervalTime(Duration timeout) {
        if (timeout == null) {
            this.intervalTime = Duration.ofMillis(1000).toMillis(); // 기본값 초기화
        } else {
            this.intervalTime = timeout.toMillis();
        }
    }

    public void setWebDriverWaitTimeout(Duration timeout) {
        if (timeout == null) {
            this.webDriverWait = new WebDriverWait(webDriver, Duration.ofMillis(2000)); // 기본값 초기화
        } else {
            this.webDriverWait = new WebDriverWait(webDriver, timeout);
        }
    }

    public void setWebDriverWait(WebDriverWait webDriverWait) {
        if (webDriverWait == null) { // default
            this.webDriverWait = new WebDriverWait(webDriver, Duration.ofMillis(2000));
        } else {
            this.webDriverWait = webDriverWait;
        }
    }

    public void clearWebDriverWait() {
        this.webDriverWait = null;
    }

    public void setExecutor(Executor executor) {
        if (executor == null) {
            this.executor = ForkJoinPool.commonPool();
        } else {
            this.executor = executor;
        }
    }

    public void setAutoQuitWebDriver(boolean autoQuitWebDriver) {
        this.autoQuitWebDriver = autoQuitWebDriver;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public void setScrapingOption(ScrapingOption scrapingOption) {
        this.scrapingOption = scrapingOption;
    }



    public DcBoardsAndComments start(ScrapeRequest scrapeRequest) {
        long startPage = scrapeRequest.getStartPage();
        long endPage = scrapeRequest.getEndPage();

        if (!isDriverAlive()) setWebDriver(null); // WebDriver 가 사용 불가능 할경우 기본드라이버 사용

        LocalDateTime startTime = LocalDateTime.now();
        ScrapeStatus scrapeStatus = new ScrapeStatus(startPage, endPage, startTime);
        DcBoardsAndComments scrapedContents = null;
        List<ScrapeFailure> failures = new ArrayList<>();
        try {
            // 스크래핑
            ScrapeResult scrapeResult = scrape(startPage, endPage, scrapingOption);
            scrapedContents = scrapeResult.getDcBoardsAndComments();
            failures = scrapeResult.getFailure();
            scrapeStatus.syncScrapeStatus(scrapeResult.getLoggingParams());
        } catch (Exception e) {
            log.error("[START ERROR] DcScraper.start() e = {}", e);
        } finally {
            LocalDateTime endTime = LocalDateTime.now();
            completeLogging(scrapeStatus, failures);
            if (autoQuitWebDriver) { this.quitDriver(); }
        }
        return scrapedContents;
    }

    public void startWithCallback(ScrapeRequest scrapeRequest, Consumer<DcBoardsAndComments> callback) {
        long startPage = scrapeRequest.getStartPage();
        long endPage = scrapeRequest.getEndPage();
        long interval = scrapeRequest.getInterval();

        if (!isDriverAlive()) setWebDriver(null); // WebDriver 가 사용 불가능 할경우 기본드라이버 사용
        log.info("pageload = {}", webDriver.manage().timeouts().getPageLoadTimeout());

        // 로깅용
        LocalDateTime startTime = LocalDateTime.now();
        ScrapeStatus scrapeStatus = new ScrapeStatus(startPage, endPage, startTime);
        List<ScrapeFailure> failuresTotal = new ArrayList<>();

        try {
            long intervalStartPage = startPage; // 각 구간별 시작페이지
            long intervalEndPage = startPage + interval - 1; // 각 구간별 끝페이지
            List<CompletableFuture<Void>> callbackFutures = new ArrayList<>(); // 콜백 대기 리스트

            while (intervalStartPage <= endPage) {
                // 구간 끝페이지가 전체 스크래핑 끝 페이지보다 클경우 조정
                intervalEndPage = Math.min(intervalEndPage, endPage);
                // 스크래핑
                ScrapeResult scrapeResult = scrape(intervalStartPage, intervalEndPage, scrapingOption);
                // 결과
                DcBoardsAndComments scrapedContents = scrapeResult.getDcBoardsAndComments();
                LoggingParams loggingParams = scrapeResult.getLoggingParams();
                List<ScrapeFailure> failures = scrapeResult.getFailure();

                // 후처리
                // 스크래핑 결과 0 (오류 등)
                if (scrapedContents.getBoards().isEmpty()) {
                    log.error("[START WITH CALLBACK ERROR] scrape result is empty, stop scraping intervalStartPage = {}, intervalEndPage = {}", intervalStartPage, intervalEndPage);
                    break;
                }
                // 로깅용 카운터들 갱신
                scrapeStatus.syncScrapeStatus(loggingParams);
                // 로깅용 실패결과 모음
                failuresTotal.addAll(failures);

                if (interval > 0 && callback != null) {
                    log.info("[START WITH CALLBACK] scraping executed callback, start = {}, end = {} interval = {}", intervalStartPage, intervalEndPage, interval);
                    callbackFutures.add(CompletableFuture.runAsync(() -> callback.accept(scrapedContents), executor));
                }
                // 구간시작과 구간끝 증가
                intervalStartPage += interval;
                intervalEndPage += interval;
            }
            if (callbackFutures.size() > 0) {
                CompletableFuture.allOf(callbackFutures.toArray(new CompletableFuture[0])).join(); // 콜백 전체 대기
            }
        } catch (Exception e) {
            // RetryExceededException 잡힘
            log.error("[START WITH CALLBACK ERROR] DcScraper.startWithCallback() e = {}", e);
        } finally {
            scrapeStatus.end();
            completeLogging(scrapeStatus, failuresTotal);
            if (autoQuitWebDriver) { this.quitDriver(); }
        }
    }

    protected void completeLogging(ScrapeStatus scrapeStatus, List<ScrapeFailure> failures) {
        LocalDateTime startTime = scrapeStatus.getStartTime();
        LocalDateTime endTime = scrapeStatus.getEndTime();
        Duration duration = Duration.between(startTime, endTime);

        long endPage = scrapeStatus.getEndPage();
        long startPage = scrapeStatus.getStartPage();
        long boardPerSecond = scrapeStatus.getTotalBoardCnt() == 0 ? 0 : duration.getSeconds() / scrapeStatus.getTotalBoardCnt();
        long executePageCount = scrapeStatus.getExecutedPageCount();
        long secondsPerPage = executePageCount == 0 ? 0 : duration.getSeconds() / (executePageCount);

        log.info("\n [SCRAPE COMPLETE] ==================================================\n" +
                        "  elaspedTime = {}h : {}m : {}s : {}millis, \n" +
                        "  time per board = {}s / board, per Page = {}s / page \n" +
                        "  startedFrom = {}, endTime = {}\n" +
                        "  page = {} ~ {} pageCount = {}, " +
                        "  expectedBoardCounter = {} \n" +
                        "  expectedCommentCounter = {} \n" +
                        "  scrapedBoardCounter = {}, scrapedCommentCounter = {}\n" +
                        " ======================================================================",
                duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart(),
                boardPerSecond, secondsPerPage,
                startTime, endTime,
                startPage, endPage, executePageCount,
                (executePageCount) * 100,
                scrapeStatus.getTotalDeletedCommentCnt() + scrapeStatus.getTotalCommentCntFromBoard(),
                scrapeStatus.getTotalBoardCnt(), scrapeStatus.getTotalCommentCnt());

        if (!failures.isEmpty()) {
            log.warn("[SCRAPE COMPLETE - FAILURES] failures.size = {} =====================================================", failures.size());
            failures.forEach(failure -> log.warn("failure = {}", failure));
            log.warn("=====================================================================================================");
        }
    }

    protected ScrapeResult scrape(long startPage, long endPage, ScrapingOption scrapingOption) throws InterruptedException {
        log.info("[SCRAPER] Start scraping from {} to {}", startPage, endPage);

        // 저장할 디씨글, 댓글 리스트, 실페 리스트 페이지 리스트
        List<DcBoard> resultBoards = new ArrayList<>();
        List<DcComment> resultComments = new ArrayList<>();
        List<ScrapeFailure> failures = new ArrayList<>();
        long resultDeletedCommentCount = 0;
        long resultCommentCntFromBoard = 0;

        // 스크래핑 시작, 끝 페이지
        Long pageNum = startPage;
        Long maxPageNum = endPage;

        // 재시도 카운터
        long retryCounter = 0; // 글 리스트, 글 상세 페이지 요소 로드 실패시 ++, maxRetryCounter 초과하면 RetryExceededException 발생
        int trElementsReloadCounter = 0; // 글 리스트 요소 누락시 ++, 3회까지 재시도 후 그대로 진행

        // 개발용 카운터
        long addedBoardCounter = 0; // resultBoards 에 추가된 글 수
        long addedBoardCommentCntTotal = 0; // resultBoards 에 board.getCommentCnt 총합
        long addedCommentCounter = 0; // resultComments 에 추가된 댓글 수
        int deletedCommentCounter = 0; // resultComments 에 추가된 '삭제된 댓글'갯수
        long cutCounter = 1L; // 개발용 단건 카운트 컷 1부터 N 까지 N 회

        while (pageNum <= maxPageNum) {
            // 글 리스트 페이지 URL 설정
            String executeUrl = baseUrl + galleryListUri + galleryIdParameterPrefix + galleryId + pageParameterPrefix + pageNum + listNumParameterPrefix + listNum;
            // 글 리스트 페이지 접속 및 파싱
            Elements trElements = openListPageAndParse(executeUrl, trElementsReloadCounter);

            // 글 리스트 페이지 로드 실패 (0개 로드)
            if (trElements.isEmpty()) {
                retryCounter = plusRetryCounter(executeUrl, pageNum, retryCounter);
                continue; // 현재 페이지 재시도
            }

            // 글 리스트 페이지 갯수 누락시 재시도 (기다렷다가, 최대 5회)
            if (trElements.size() < Integer.parseInt(listNum) && trElementsReloadCounter < 5) {
                log.warn("[SCRAPER] listContinue trElements.size = {}, pageNum = {} ", trElements.size(), pageNum);
                Thread.sleep(1100);
                retryCounter = plusRetryCounter(executeUrl, pageNum, retryCounter);
                trElementsReloadCounter++;
                continue; // 현재 페이지 재시도
            }

            // 글 리스트 에서 글 마다 내용 추출
            List<String> hrefs = new ArrayList<>(); // 글의 href 저장할 리스트
            List<DcBoard> extractedBoards = new ArrayList<>(); // 글 리스트에서 추출한 DcBoard 리스트
            for (Element trElement : trElements) {
                DcBoard extractingBoard = boardExtractor.extractFromListPage(trElement);
                if (extractingBoard.getDcNum() == -1) {
                    continue; // 공지, AD, 제휴글 등의 이유로 건너뛰어짐
                } else {
                    // a 태그에서 글 href 추출 (/board/view/?id=granblue&no=4803525&page=1)
                    hrefs.add(trElement.select(boardHrefSelector).attr("href"));
                    extractedBoards.add(extractingBoard);
                }
            }
            resultBoards.addAll(extractedBoards);
            addedBoardCounter += extractedBoards.size();

            // 리스트 url 3회이상 실패후 진행시 누락진행으로 실패리스트에 추가
            if (trElementsReloadCounter >= 5) {
                failures.add(new ScrapeFailure(executeUrl, trElements.size(), extractedBoards.size(), pageNum));
            }

            // 페이지 추출후 초기화
            trElementsReloadCounter = 0;
            retryCounter = 0;

            // ScrapingOption.LISTPAGE 종료지점 =================================================
            if (scrapingOption != ScrapingOption.LISTPAGE) {

                // 글 리스트에서 글 하나하나 순회시작
                int boardIndex = 0; // 글 순회 인덱스.
                while (boardIndex < extractedBoards.size()) {
                    DcBoard extractingBoard = extractedBoards.get(boardIndex);
                    String href = hrefs.get(boardIndex);

                    executeUrl = baseUrl + href;

                    // 글 상세 페이지 접속 및 파싱
                    Element mainElement = openViewPageAndParse(executeUrl);
                    if (mainElement == null) {
                        retryCounter = plusRetryCounter(executeUrl, pageNum, retryCounter);
                        continue; // 현재 글 재시도
                    }

                    // 상세 페이지에서 내용 추출 후 DcBoard 객체에 저장하여 완성
                    String rawContent = boardExtractor.extractContentFromViewPage(mainElement);
                    extractingBoard.setContent(rawContent);

                    // 완성 후 로깅
                    String cleanContent = ContentCleaner.cleanContent(rawContent);
                    log.info("\n================================================================\n" +
                                    " title = \n" +
                                    "   {}\n" +
                                    " cleanContent = \n" +
                                    "   {}\n" +
                                    "================================================================\n",
                            extractingBoard.getTitle(),
                            cleanContent
                    );
                    // ScrapingOption.VIEWPAGE 종료지점 ============================================

                    // ScrapingOption.ALL 일 경우 댓글 까지 추출
                    if (scrapingOption == ScrapingOption.ALL) {
                        // 상세 페이지에서 댓글 추출
                        List<DcComment> extractedComments = extractCommentsFromViewPage(extractingBoard.getDcNum(), mainElement);
                        // 완성된 List<DcComment> 객체 저장
                        resultComments.addAll(extractedComments);

                        // 삭제된 댓글 갯수 ( regDate = null )
                        int deletedCommentCount = extractedComments.stream().filter(dcComment -> dcComment.getRegDate() == null).collect(Collectors.toList()).size();

                        addedCommentCounter += extractedComments.size() - deletedCommentCount;
                        deletedCommentCounter += deletedCommentCount;
                        resultDeletedCommentCount += deletedCommentCount;

                        addedBoardCommentCntTotal += extractingBoard.getCommentCnt();
                        resultCommentCntFromBoard += extractingBoard.getCommentCnt();


                        // 댓글 개수 체크 (댓글돌이는 extractingBoard.getCommentCnt() 숫자에서 제외됨, 삭제된 댓글 갯수는 빼줘야됨)
                        if (extractingBoard.getCommentCnt() != extractedComments.size() - deletedCommentCount) {
                            log.warn("[COMMENT.PROPERLY] comment not extracted properly extractingBoard.getCommentCnt() = {} extractedComments.size() = {}, deletedCommentCount = {}, executeUrl={}", extractingBoard.getCommentCnt(), extractedComments.size(), deletedCommentCount, executeUrl);
                            extractedComments.forEach(dcComment -> log.warn("dcComment = {}", dcComment));
                        }
                    }
                    // SrapingOption.ALL 종료지점 ===================================================

                    // 다음 글로
                    boardIndex++;

                    // 글 하나만 하고 끝내기
//                break;

                    // 컷 카운터로 컷
//                if (cutCounter >= 3) break;
//                cutCounter++;

                } // for trElements
            }

            log.info("\n[DCSCRAPER - PAGE END] =================================================================\n" +
                            "pageNum = {}/{}]\n" +
                            "addedBoardCounter = {} trElements.size = {} \n" +
                            "addedCommentCounter = {} \n" +
                            "addedBoardCommentCntTotal = {} == calculated expect = {} \n" +
                            "===========================================================================================",
                    pageNum, maxPageNum,
                    addedBoardCounter, trElements.size(),
                    addedCommentCounter,
                    addedBoardCommentCntTotal, addedCommentCounter - deletedCommentCounter);

            // 자꾸 글 흘려서 확인용 (1 페이지의 경우 공지글로 인해 갯수가 listNum 미만으로 줄어든다)
            if (addedBoardCounter != Integer.parseInt(listNum)) {
                log.warn("[BOARD.PROPERLY] boardNotAdded properly. listnum = {} addedBoardCounter = {} resultBoards.size = {}", listNum, addedBoardCounter, resultBoards.size());
            }

            // 다음 페이지로
            pageNum++;

            // 카운터 초기화
            addedBoardCounter = 0;
            addedCommentCounter = 0;
            addedBoardCommentCntTotal = 0;
            deletedCommentCounter = 0;
            cutCounter = 1L;

        }// for pageNum
        return ScrapeResult.builder()
                .dcBoardsAndComments(new DcBoardsAndComments(resultBoards, resultComments))
                .failure(failures)
                .loggingParams(LoggingParams.builder()
                        .scrapedPageCnt(maxPageNum - startPage + 1)
                        .boardPerPage(Integer.parseInt(listNum))
                        .scrapedBoardCount(resultBoards.size())
                        .scrapedCommentCnt(resultComments.size())
                        .scrapedDeletedCommentCnt(resultDeletedCommentCount)
                        .scrapedBoardCommentCntTotal(resultCommentCntFromBoard)
                        .build()
                ).build();
    }

    /**
     * WebDriver 를 통해 글 리스트 페이지를 열고, 파싱하여 글 리스트 Elements 를 반환함
     *
     * @param executeUrl
     * @param trElementsReloadCounter 재시도 카운터
     * @return 글 리스트의 tr 요소로 구성된 Elements 객체, 실패시 size = 0 반환
     */
    protected Elements openListPageAndParse(String executeUrl, int trElementsReloadCounter) {
        Elements results = new Elements();
        try {
            Thread.sleep(intervalTime);

            log.info("[DRIVER] opening listPage executeUrl = {}", executeUrl);
            // 글 리스트 ul 요소 획득 및 파싱 Selenium
            webDriver.get(executeUrl);

            if (trElementsReloadCounter > 0) {
                // 리스트 페이지의 글 갯수가 누락되어 재시도 중
                long sleepTime = 1 + trElementsReloadCounter; // 재시도 카운터에 따라 대기시간 증가
                Thread.sleep(sleepTime); // 리스트 페이지 로드 기다림
            }

            WebElement gallList = waitUntilElementLocated(By.cssSelector(boardListSelector));
            String gallListOuterHtml = gallList.getAttribute("outerHTML");
            Element tableElement = Jsoup.parse(gallListOuterHtml);
            results = tableElement.select(boardListItemSelector);
        } catch (Exception e) {
            log.error("[ERROR] webDriver.get(executeUrl);  executeUrl = {} e.name = {} ", executeUrl, e.getClass().getName());
        }
        return results;
    }

    /**
     * WebDriver 를 통해 글 상세 페이지를 열고, 파싱하여 본문 + 댓글 리스트를 포함하는 main 요소를 반환함
     *
     * @param executeUrl
     * @return 본문 + 댓글 리스트를 포함하는 main 요소 Element 객체, 실패시 null 반환
     */
    protected Element openViewPageAndParse(String executeUrl) {
        Stopwatch stopwatch; // 개발용 스톱워치
        Element result; // 상세페이지의 main 요소 담길 변수
        try {
            Thread.sleep(intervalTime); // boardInterval.  여기에 없으면 차단당하거나 로드 느려짐 (로드 후 기다리기X)

            stopwatch = Stopwatch.createStarted();
            log.info("[DRIVER] opening viewPage executeUrl = {}", executeUrl);
            webDriver.get(executeUrl);

            WebElement mainContainer; // 글 내용 + 댓글 합친 요소
            try {
                // 기본적으로 기다릴 요소
                mainContainer = waitUntilElementLocated(By.cssSelector(boardViewSelector));
            } catch (Exception e) {
                // 야간 시간대에는 위가 아니라 이걸로 찾아야 됨. (불확실)
                mainContainer = waitUntilElementLocated(By.cssSelector(boardViewSelectorAlter));
            }

            // 가끔 로드 느려서 에러나기 떄문에 더 기다림
            waitUntilElementLocated(By.cssSelector(boardViewContentSelector));

            // 파싱
            String mainContainerHtml = mainContainer.getAttribute("outerHTML");
            result = Jsoup.parse(mainContainerHtml);

            stopwatch.stop();
            log.info("[STOPWATCH] get mainElement from page : stopwatch.elapsed = {} : {}", stopwatch.elapsed().toSeconds(), stopwatch.elapsed().toMillisPart());
        } catch (Exception e) {
            log.error("[ERROR] webDriver.get(executeUrl);  executeUrl = {} e.name = {}", executeUrl, e.getClass().getName());
            result = null;
        }
        return result;
    }

    /**
     * 본문내용 + 댓글 요소를 포함하는 mainElement 로부터 댓글 리스트를 추출하여 반환
     *
     * @param mainElement
     * @param dcNum
     * @return
     */
    public List<DcComment> extractCommentsFromViewPage(long dcNum, Element mainElement) {
        List<DcComment> results = new ArrayList<>();

        // 댓글 리스트 ul 요소 추출
        Element ulElementComment = mainElement.select(commentListSelector).first();
        // 댓글 있으면 내용 추출
        if (ulElementComment != null) {
            // 댓글 리스트 내부 li 요소 (댓글 + 답글)
            Elements liElementsComment = ulElementComment.select(commentListItemSelector);
            Element liElementPrev = null; // 답글의 target 설정을 위한 직전 반복 댓글
            for (Element liElement : liElementsComment) {
                List<DcComment> extractedComments = commentExtractor.extractCommentAndReply(dcNum, liElement, liElementPrev);
                results.addAll(extractedComments);
                liElementPrev = liElement;
            }
        }
        return results;
    }

    /**
     * WebdriverWait 의 상태에 맞춰 wait 후 WebElement 반환
     * @param by
     * @return
     */
    protected WebElement waitUntilElementLocated(By by) {
        WebElement result;
        if (webDriverWait == null) {
            // 사용자가 implictWait 등을 설정한 경우
            result = webDriver.findElement(by);
        } else {
            result = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(by));
        }
        return result;
    }

    /**
     * 실패 로그 찍고 retryCounter 를 증가시켜 반환
     *
     * @param executeUrl
     * @param pageNum
     * @param retryCounter
     * @return retryCounter++
     * @throws RetryExceededException retryCounter 가 maxRetryCount 를 초과할 경우 던짐
     */
    protected long plusRetryCounter(String executeUrl, long pageNum, long retryCounter) {
        log.error("[DCSCRAPER] plusRetryCounter() executeUrl = {}, pageNum = {}, retryCounter = {}", executeUrl, pageNum, retryCounter);
        retryCounter++;
        if (retryCounter > maxRetryCount) {
            throw new RetryExceededException("retryCounter exceeded, stop scraping, retryCounter = " + retryCounter +
                    " Exception pageNum = " + pageNum);
        }
        return retryCounter;
    }
}

/*
디시 글 갯수
페이지당 50개, 1페이지의 경우 공지글을 포함하여 50개.
그랑블루갤러리의 경우 현재 공지 2개
48 + 50 * N
 */


