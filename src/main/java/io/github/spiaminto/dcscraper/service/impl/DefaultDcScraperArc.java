package io.github.spiaminto.dcscraper.service.impl;//package kr.granblue.dcscraper.scraper.service.impl;
//
//
//import com.google.common.base.Stopwatch;
//import kr.granblue.dcscraper.scraper.dto.DcBoard;
//import kr.granblue.dcscraper.scraper.dto.DcBoardsAndComments;
//import kr.granblue.dcscraper.scraper.dto.DcComment;
//import kr.granblue.dcscraper.scraper.dto.ScrapeRequest;
//import kr.granblue.dcscraper.scraper.enums.ScrapingOption;
//import kr.granblue.dcscraper.scraper.exception.RetryExceededException;
//import kr.granblue.dcscraper.scraper.extractor.BoardExtractor;
//import kr.granblue.dcscraper.scraper.extractor.CommentExtractor;
//import kr.granblue.dcscraper.scraper.service.DcScraper;
//import kr.granblue.dcscraper.scraper.util.WebDriverUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.openqa.selenium.By;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.WebDriverWait;
//import org.springframework.scheduling.annotation.Async;
//
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.Executor;
//import java.util.concurrent.ForkJoinPool;
//import java.util.function.Consumer;
//
//@Slf4j
//public class DefaultDcScraperArc implements DcScraper {
//    private final CommentExtractor commentExtractor;
//    private final BoardExtractor boardExtractor;
//
//    private Executor defaultExecutor;
//    private WebDriver globalWebDriver;
//    private WebDriverWait globalExplicitWait;
//    private Executor globalExecutor;
//
//    private ScrapingOption scrapingOption;
//
//    public DefaultDcScraperArc(BoardExtractor boardExtractor, CommentExtractor commentExtractor) {
//        this.boardExtractor = boardExtractor;
//        this.commentExtractor = commentExtractor;
//
//        setWebDriver(null); // 기본값으로 초기화 (driverWait 과 같이)
//
//        defaultExecutor = ForkJoinPool.commonPool(); // default CompletableFuture executor
//        globalExecutor = defaultExecutor;
//
//        scrapingOption = ScrapingOption.ALL;
//    }
//
//    // 개발용 카운터
//    private long addedBoardCounter = 0;
//    private long addedCommentCounter = 0;
//    private long cutCounter = 1L; // 개발용 단건 카운트 컷 1부터 N 까지 N 회
//
//    /* properties */
//    // 스크래핑 할 url, 가변 uri
//    private String baseUrl = "http://gall.dcinside.com";
//    private String galleryListUri = "/board/lists/";
//    private String minorGalleryListUri = "/mgallery/board/lists/";
//    private String galleryViewUri = "/board/view/"; // 안씀
//    private String galleryNameParameter = "?id=granblue"; // 반드시 첫번째로 사용하는 갤러리 이름 파라미터
//    private String galleryNameParameterPrefix = "?id="; // 반드시 첫번째로 사용하는 갤러리 이름 파라미터
//    private String pageParameter = "&page="; // 페이징 파라미터
//    private String listNumParameter = "&list_num=100";
//
//    private String searchParameter = "&s_type=search_subject_memo&s_keyword="; // 검색 파라미터(제목+내용)
//    private String boardListSelector = ".gall_list";
//    private String boardListItemSelector = "tbody tr";
//    private String boardHrefSelector = ".gall_tit>a";
//    private String boardViewSelector = ".gallery_view";
//    private String boardViewSelectorAlter = "#container";
//    private String boardViewContentSelector = ".write_div"; // 글 내용 로드 기다릴때 사용
//    private String commentListSelector = ".cmt_list";
//    private String commentListItemSelector = ".cmt_list>li";
//    private long maxRetryCount = 10;
//
//    public void setWebDriver(WebDriver webDriver) {
//        // WebDriver set 전 기존 driver 가 있을 경우 닫기
//        if (this.globalWebDriver != null) {
//            this.globalWebDriver.close();
//            this.globalWebDriver.quit();
//        }
//        if (webDriver == null) {
//            this.globalWebDriver = WebDriverUtil.getChromeDriver();
//            // 기본 타이머로 초기화
//            setWebDriverWait(null);
//        } else {
//            this.globalWebDriver = webDriver;
//            // 타이머 해제
//            clearWebDriverWait();
//        }
//    }
//
//    public void setWebDriverWait(WebDriverWait explicitWait) {
//        if (explicitWait == null) {
//            this.globalExplicitWait = new WebDriverWait(globalWebDriver, Duration.ofMillis(2000));
//        } else {
//            this.globalExplicitWait = explicitWait;
//        }
//    }
//
//    public void clearWebDriverWait() {
//        this.globalExplicitWait = null;
//    }
//
//    public void setExecutor(Executor executor) {
//        if (executor == null) {
//            this.globalExecutor = defaultExecutor;
//        } else {
//            this.globalExecutor = executor;
//        }
//    }
//
//    public void setScrapingOption(ScrapingOption scrapingOption) {
//        this.scrapingOption = scrapingOption;
//    }
//
//    public void quitDriver() {
//        if (globalWebDriver != null) {
//            globalWebDriver.close();
//            globalWebDriver.quit();
//        }
//    }
//
//    public DcBoardsAndComments start(ScrapeRequest scrapeRequest) {
//        long startPage = scrapeRequest.getStartPage();
//        long endPage = scrapeRequest.getEndPage();
//
//        DcBoardsAndComments result = null;
//        try {
//            // 스크래핑
//            result = scrape(startPage, endPage, globalWebDriver, scrapingOption);
//        } catch (Exception e) {
//            log.error("[START ERROR] DcScraper.start() e = {}", e);
//        }
//        return result;
//    }
//
//    public void startWithCallback(ScrapeRequest scrapeRequest, Consumer<DcBoardsAndComments> callback) {
//        long startPage = scrapeRequest.getStartPage();
//        long endPage = scrapeRequest.getEndPage();
//        long interval = scrapeRequest.getInterval();
//
//        try {
//            long intervalStartPage = startPage; // 각 구간별 시작페이지
//            long intervalEndPage = startPage + interval - 1; // 각 구간별 끝페이지
//            List<CompletableFuture<Void>> callbackFutures = new ArrayList<>(); // 콜백 대기 리스트
//
//            while (intervalStartPage <= endPage) {
//                // 구간 끝페이지가 전체 스크래핑 끝 페이지보다 클경우 조정
//                intervalEndPage = Math.min(intervalEndPage, endPage);
//                // 스크래핑
//                DcBoardsAndComments scrapeResult = scrape(intervalStartPage, intervalEndPage, globalWebDriver, scrapingOption);
//                // 스크래핑 결과 0 (오류 등)
//                if (scrapeResult.getBoards().isEmpty()) {
//                    log.error("[START WITH CALLBACK ERROR] scrape result is empty, stop scraping intervalStartPage = {}, intervalEndPage = {}", intervalStartPage, intervalEndPage);
//                    break;
//                }
//                // 콜백 실행
//                Executor executor = globalExecutor == null ? defaultExecutor : globalExecutor;
//                if (interval > 0 && callback != null) {
//                    log.info("[START WITH CALLBACK] scraping executed callback, start = {}, end = {} interval = {}", intervalStartPage, intervalEndPage, interval);
//                    callbackFutures.add(CompletableFuture.runAsync(() -> callback.accept(scrapeResult), executor));
//                }
//                // 구간시작과 구간끝 증가
//                intervalStartPage += interval;
//                intervalEndPage += interval;
//            }
//            if (callbackFutures.size() > 0) {
//                CompletableFuture.allOf(callbackFutures.toArray(new CompletableFuture[0])).join(); // 콜백 전체 대기
//            }
//        } catch (Exception e) {
//            // RetryExceededException 잡힘
//            log.error("[START WITH CALLBACK ERROR] DcScraper.startWithCallback() e.getMessage = {}, \n" +
//                    "e = {}", e.getMessage(), e);
//        }
//    }
//
//    @Async
//    public CompletableFuture<Void> asyncStartWithCallback(ScrapeRequest scrapeRequest, Consumer<DcBoardsAndComments> callback) {
//        Executor executor = globalExecutor == null ? defaultExecutor : globalExecutor;
//        return CompletableFuture.runAsync(() -> {
//            startWithCallback(scrapeRequest, callback);
//        }, executor);
//    }
//
//    protected DcBoardsAndComments scrape(long startPage, long endPage, WebDriver webDriver, ScrapingOption option) throws InterruptedException {
//        log.info("[SCRAPER] Start scraping from {} to {}", startPage, endPage);
//
//        // 저장할 디씨글, 댓글 리스트
//        List<DcBoard> resultBoards = new ArrayList<>();
//        List<DcComment> resultComments = new ArrayList<>();
//
//        // 스크래핑 시작, 끝 페이지
//        Long pageNum = startPage;
//        Long maxPageNum = endPage;
//
//        long retryCounter = 0; // 글 리스트, 글 상세 페이지 로드 Exception 발생시 ++, 같은 페이지 10회 초과 실패시 stop
//        int trElementsReloadCounter = 0;
//
//        while (pageNum <= maxPageNum && retryCounter <= maxRetryCount) {
//            // 글 리스트 페이지 접속
//            String executeUrl = baseUrl + galleryListUri + galleryNameParameter + pageParameter + pageNum + listNumParameter;
//            Document listPageDocument; // 리스트 페이지 document
//            Element tableElement; // 리스트 페이지 테이블 (글 테이블)
//            Elements trElements; // 글 리스트 tr 요소
//
//            Elements trElements = openListPageAndParse(executeUrl);
//
//            if ( (trElements == null || trElements.size() < 100) &&
//                    trElementsReloadCounter < 3) {
//                // 글 흘리는 경우 파악을 위해. 최대 3회 기다렷다 리로드
//                log.warn("[SCRAPER] listContinue trElements.size = {}, pageNum = {} ",
//                        trElements != null ? trElements.size() : "null", pageNum);
//                Thread.sleep(500);
//                trElementsReloadCounter++;
//                continue;
//            }
//            trElementsReloadCounter = 0;
//
//            try {
//                log.info("[DRIVER] opening listPage executeUrl = {}", executeUrl);
//
//                // 글 리스트 ul 요소 획득 및 파싱 Selenium
//                webDriver.get(executeUrl);
//                WebElement gallList = waitUntilElementLocated(webDriver, By.cssSelector(boardListSelector));
//                String gallListOuterHtml = gallList.getAttribute("outerHTML");
//                tableElement = Jsoup.parse(gallListOuterHtml);
//                trElements = tableElement.select(boardListItemSelector);
//
//                if (trElements.size() < 100 && trElementsReloadCounter < 3) {
//                    // 가끔 글 갯수를 흘려서 재로드
//                    // 미리 브라우저로 해본 결과 재로드 해도, 기다려도 디시 자체의 버그인지 100개가 안되는 경우가 간혹있음
//                    // 글 자체는 url 에 boardNum 변경해서 넣으면 찾을수 있으나 리스트에서는 찾을수 없음 원인불명
//                    log.warn("[DCSCRAPER] trElements.size() < 100, trElements.size() = {} pageNum = {}", trElements.size(), pageNum);
//                    Thread.sleep(500);
//                    trElementsReloadCounter++;
//                    continue;
//                }
//                trElementsReloadCounter = 0;
//
//            } catch (Exception e) {
//                log.error("[ERROR] webDriver.get(executeUrl);  executeUrl = {} e.name = {} retryCounter = {}", executeUrl, e.getClass().getName(), retryCounter);
//                retryCounter++;
//                // 재시도 횟수 초과 -> Exception 발생 및 스크래핑 종료
//
//                continue; // 재 로드
//            }
//
//            retryCounter = 0; // 페이지 로드 성공시 초기화
//
//            // 글마다 루프 (글 열기 실패시 재 반복을 위해 while)
//            int boardLoopIndex = 0;
//            while (boardLoopIndex < trElements.size() && retryCounter <= maxRetryCount) {
//                Element trElement = trElements.get(boardLoopIndex);
//
//                // 글 리스트 에서 글 마다 내용 추출
//                DcBoard extractingBoard = boardExtractor.extractFromListPage(trElement);
//                if (extractingBoard.getDcNum() == -1) {
//                    boardLoopIndex++;
//                    continue;
//                } // 공지, AD 등의 이유로 건너뛰어짐
//
//                // a 태그에서 글 href 추출 (/board/view/?id=granblue&no=4803525&page=1)
//                String href = trElement.select(boardHrefSelector).attr("href");
//                executeUrl = baseUrl + href;
//
//                // 글 상세 페이지 접속
//                Stopwatch stopwatch; // 개발용 스톱워치
//                Element mainElement; // 상세페이지의 main 요소 담길 변수
//                try {
//                    Thread.sleep(300); // boardInterval.  여기에 없으면 차단당하거나 로드 느려짐 (로드 후 기다리기X)
//
//                    stopwatch = Stopwatch.createStarted();
//                    log.info("[DRIVER] opening viewPage executeUrl = {}", executeUrl);
//                    webDriver.get(executeUrl);
//
//
//                    WebElement mainContainer; // 글 내용 + 댓글 합친 요소
//                    try {
//                        // 기본적으로 기다릴 요소
//                        mainContainer = waitUntilElementLocated(webDriver, By.cssSelector(boardViewSelector));
//                    } catch (Exception e) {
//                        // 야간 시간대에는 위가 아니라 이걸로 찾아야 됨. (불확실)
//                        mainContainer = waitUntilElementLocated(webDriver, By.cssSelector(boardViewSelectorAlter));
//                    }
//                    // 가끔 로드 느려서 에러나기 떄문에 더 기다림
//                    waitUntilElementLocated(webDriver, By.cssSelector(boardViewContentSelector));
//                    String mainContainerHtml = mainContainer.getAttribute("outerHTML");
//                    mainElement = Jsoup.parse(mainContainerHtml);
//
//                    // headless 해제 후 엘리먼트 직접 확인용
////                    Thread.sleep(100000);
//
//                    stopwatch.stop();
//                    log.info("[STOPWATCH] get mainElement from page : stopwatch.elapsed = {} : {}", stopwatch.elapsed().toSeconds(), stopwatch.elapsed().toMillisPart());
//
//                } catch (Exception e) {
//                    log.error("[ERROR] webDriver.get(executeUrl);  executeUrl = {} e.name = {}, retryCounter = {}", executeUrl, e.getClass().getName(), retryCounter);
//                    retryCounter++;
//                    // 재시도 횟수 초과 -> Exception 발생 및 스크래핑 종료
//                    if (retryCounter > maxRetryCount) {
//                        log.error("RetryExceeded currentPage = {}", pageNum);
//                        throw new RetryExceededException("retryCounter exceeded, stop scraping, retryCounter = {}" + retryCounter +
//                                "startPage = " + startPage + "endPage = " + endPage, e);
//                    }
//                    continue; // 재 로드
//                }
//
//                retryCounter = 0; // 페이지 로드 성공시 초기화
//
//                // 상세 페이지에서 내용 추출
//                DcBoard extractedBoard = boardExtractor.extractContentFromViewPage(extractingBoard, mainElement);
//
//                // board 추출 완료
//                resultBoards.add(extractedBoard);
//                addedBoardCounter++;
//
//                // 댓글 요소 추출
//                Element ulElementComment = mainElement.select(commentListSelector).first(); // 댓글리스트 ul 요소
//
//                // 댓글 있으면 내용 추출
//                long beforeResultCommentSize = resultComments.size();
//                if (ulElementComment != null) {
//                    Elements liElementsComment = ulElementComment.select(commentListItemSelector); // 댓글 리스트 내부 li 요소 (댓글 + 답글)
//                    Element liElementPrev = null; // 답글의 target 설정을 위한 직전 반복 댓글
//                    for (Element liElement : liElementsComment) {
//                        List<DcComment> extractedComments = commentExtractor.extractCommentAndReply(extractedBoard.getDcNum(), liElement, liElementPrev);
//                        resultComments.addAll(extractedComments);
//                        addedCommentCounter += extractedComments.size();
//                        liElementPrev = liElement;
//                    }
//                }
//                long afterResultCommentSize = resultComments.size();
//                long extractedCommentsSizeFromBoard = afterResultCommentSize - beforeResultCommentSize;
//
//                boardLoopIndex++;
//                if (extractedBoard.getCommentCnt() != extractedCommentsSizeFromBoard) {
//                    log.warn("[DCSCRAPER] comment not extracted properly executeUrl = {} extractedBoard.getCommentCnt() = {} resultComments.size() = {}",executeUrl, extractedBoard.getCommentCnt(),extractedCommentsSizeFromBoard);
//                }
//
//                // 글 하나만 하고 끝내기 ===============
////                break;
//
////                 컷 카운터로 컷
////                if (cutCounter >= 10) break;
////                cutCounter++;
//
//                if (retryCounter > maxRetryCount) {
//                    throw new RetryExceededException("retryCounter exceeded, stop scraping, retryCounter = {}" + retryCounter +
//                            "startPage = " + startPage + "endPage = " + endPage);
//                }
//
//            } // for boardElement
//
//            log.info("\n[DCSCRAPER - PAGE END] =================================================================\n" +
//                            "pageNum = {}/{}]\n" +
//                            "addedBoardCounter = {} addedCommentCounter = {}\n" +
//                            "resultBoards.size = {} resultComment.size = {}\n" +
//                            "trElements.size = {}" +
//                            "===========================================================================================",
//                    pageNum, maxPageNum, addedBoardCounter, addedCommentCounter, resultBoards.size(), resultComments.size(), trElements.size());
//
//            // 자꾸 글 흘려서 확인용
//            if (addedBoardCounter != 100) {
//                log.warn("[DCSCRAPER] boardNotAdded properly (100) addedBoardCounter = {} resultBoards.size = {}", addedBoardCounter, resultBoards.size());
//            }
//
//            pageNum++;
//            addedBoardCounter = 0;
//            addedCommentCounter = 0;
//
//            cutCounter = 1L;
//
//        }// for pageNum
//
//        return new DcBoardsAndComments(resultBoards, resultComments);
//    }
//
//    /**
//     * WebDriver 를 통해 글 리스트 페이지를 열고, 파싱하여 글 리스트 Elements 를 반환함
//     * @param executeUrl
//     * @return 글 리스트의 tr 요소로 구성된 Elements
//     */
//    protected Elements openListPageAndParse(String executeUrl) {
//        Elements results = null;
//        try {
//            log.info("[DRIVER] opening listPage executeUrl = {}", executeUrl);
//            // 글 리스트 ul 요소 획득 및 파싱 Selenium
//            globalWebDriver.get(executeUrl);
//            WebElement gallList = waitUntilElementLocated(globalWebDriver, By.cssSelector(boardListSelector));
//            String gallListOuterHtml = gallList.getAttribute("outerHTML");
//            Element tableElement = Jsoup.parse(gallListOuterHtml);
//            results = tableElement.select(boardListItemSelector);
//        } catch (Exception e) {
//            log.error("[ERROR] webDriver.get(executeUrl);  executeUrl = {} e.name = {} ", executeUrl, e.getClass().getName());
//        }
//        return results;
//    }
//
//    /**
//     * WebdriverWait 의 상태에 맞춰 wait 후 WebElement 반환
//     *
//     * @param webDriver
//     * @param by
//     * @return
//     */
//    protected WebElement waitUntilElementLocated(WebDriver webDriver, By by) {
//        WebElement result;
//        if (globalExplicitWait == null) {
//            // 사용자가 implictWait 등을 설정한 경우
//            result = webDriver.findElement(by);
//        } else {
//            result = globalExplicitWait.until(ExpectedConditions.presenceOfElementLocated(by));
//        }
//        return result;
//    }
//}
//
///*
//디시 글 갯수
//페이지당 50개, 1페이지의 경우 공지글을 포함하여 50개.
//그랑블루갤러리의 경우 현재 공지 2개
//48 + 50 * N
// */
//
//
