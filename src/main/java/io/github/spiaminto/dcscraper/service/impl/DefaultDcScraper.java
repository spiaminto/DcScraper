package io.github.spiaminto.dcscraper.service.impl;


import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import io.github.spiaminto.dcscraper.dto.*;
import io.github.spiaminto.dcscraper.enums.GalleryType;
import io.github.spiaminto.dcscraper.enums.ScrapingOption;
import io.github.spiaminto.dcscraper.exception.RetryExceededException;
import io.github.spiaminto.dcscraper.extractor.BoardExtractor;
import io.github.spiaminto.dcscraper.extractor.CommentExtractor;
import io.github.spiaminto.dcscraper.properties.ScraperProperties;
import io.github.spiaminto.dcscraper.service.DcScraper;
import io.github.spiaminto.dcscraper.util.ContentCleaner;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StopWatch;

import java.sql.Time;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

//TODO galleryId 를 ScrapeRequest 에서 받아오도록 수정
@Slf4j
public class DefaultDcScraper implements DcScraper {
    private final CommentExtractor commentExtractor;
    private final BoardExtractor boardExtractor;
    private final ScraperProperties props;
    private Executor executor; // callback executor
    private Page browserPage; // 웹 드라이버

    // 런타임 옵션
    private ScrapingOption scrapingOption;
    private String galleryId;
    private boolean isMinorGallery;
    private int maxRetryCount; // 한 페이지(또는 요소 탐색) 에 연속으로 실패시 중단할 최대 카운트
    private long cutCounter; // 개발용 컷카운터


    public DefaultDcScraper(BoardExtractor boardExtractor, CommentExtractor commentExtractor, ScraperProperties scraperProperties) {
        this.boardExtractor = boardExtractor;
        this.commentExtractor = commentExtractor;
        this.props = scraperProperties;

        executor = ForkJoinPool.commonPool(); // default CompletableFuture executor
        scrapingOption = ScrapingOption.ALL;
        cutCounter = 0;
    }


    public void setExecutor(Executor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor is null");
        }
        this.executor = executor;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public void setScrapingOption(ScrapingOption scrapingOption) {
        this.scrapingOption = scrapingOption;
    }

    public void setCutCounter(long cutCounter) {
        this.cutCounter = cutCounter;
    }


    public DcBoardsAndComments start(ScrapeRequest scrapeRequest) {
        galleryId = scrapeRequest.getGalleryId();
        isMinorGallery = scrapeRequest.getGalleryType() == GalleryType.MINOR;
        long startPage = scrapeRequest.getStartPage();
        long endPage = scrapeRequest.getEndPage();

        LocalDateTime startTime = LocalDateTime.now();
        ScrapeStatus scrapeStatus = new ScrapeStatus(startPage, endPage, startTime);
        List<ScrapeFailure> failures = new ArrayList<>();
        DcBoardsAndComments scrapedContents = null;

        try {
            // 스크래핑
            ScrapeResult scrapeResult = scrape(startPage, endPage, scrapingOption);
            // 스크래핑 결과 처리
            scrapedContents = scrapeResult.getDcBoardsAndComments();
            failures = scrapeResult.getFailure();
            scrapeStatus.syncScrapeStatus(scrapeResult.getLoggingParams());

        } catch (Exception e) {
            log.error("[START ERROR] DcScraper.start() e = {}", e.getClass().getName());
        } finally {
            // 종료로깅
            scrapeStatus.end();
            completeLogging(scrapeStatus, failures);
        }
        return scrapedContents;
    }

    public void startWithCallback(ScrapeRequest scrapeRequest, Consumer<DcBoardsAndComments> callback) {
        galleryId = scrapeRequest.getGalleryId();
        isMinorGallery = scrapeRequest.getGalleryType() == GalleryType.MINOR;
        long startPage = scrapeRequest.getStartPage();
        long endPage = scrapeRequest.getEndPage();
        long interval = scrapeRequest.getInterval();

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
                // 로깅용 카운터들 갱신
                scrapeStatus.syncScrapeStatus(loggingParams);
                // 로깅용 실패결과 모음
                failuresTotal.addAll(failures);
                // 콜백 실행
                if (interval > 0 && callback != null) {
                    log.debug("[START WITH CALLBACK] scraping executed callback, start = {}, end = {} interval = {}", intervalStartPage, intervalEndPage, interval);
                    callbackFutures.add(CompletableFuture.runAsync(() -> callback.accept(scrapedContents), executor));
                }
                // 구간시작과 구간끝 증가
                intervalStartPage += interval;
                intervalEndPage += interval;
            }
            // 콜백 대기
            if (!callbackFutures.isEmpty()) {
                CompletableFuture.allOf(callbackFutures.toArray(new CompletableFuture[0])).join(); // 콜백 전체 대기
            }

        } catch (Exception e) {
            log.error("[START WITH CALLBACK ERROR] DcScraper.startWithCallback() e = {}, message = {}", e.getClass().getName(), e.getMessage());
        } finally {
            // 종료 로깅
            scrapeStatus.end();
            completeLogging(scrapeStatus, failuresTotal);
        }
    }

    protected ScrapeResult scrape(long startPage, long endPage, ScrapingOption scrapingOption) {
        log.info("[SCRAPER] Start scraping from {} to {}", startPage, endPage);

        // 저장할 디씨글, 댓글 리스트, 실페 리스트 페이지 리스트
        List<DcBoard> resultBoards = new ArrayList<>(); // 반환할 글
        List<DcComment> resultComments = new ArrayList<>(); // 반환할 댓글
        List<ScrapeFailure> failures = new ArrayList<>();  // 반환할 글 일부 누락된 페이지 정보
        long resultDeletedCommentCount = 0; // LoggingParam (댓글수) 갯수 합
        long resultCommentCntFromBoard = 0; // LoggingParam '삭제된 댓글' 갯수 합

        // 스크래핑 시작, 끝 페이지
        Long pageNum = startPage;
        Long maxPageNum = endPage;

        // 재시도 카운터
        long retryCounter = 0; // 글 리스트, 글 상세 페이지 요소 로드 실패시 ++, maxRetryCounter 초과하면 RetryExceededException 발생
        long trElementsReloadCounter = 0; // 글 리스트 요소 누락시 ++, 3회까지 재시도 후 그대로 진행

        // 페이지 로깅용 카운터
        long addedBoardCount = 0; // resultBoards 에 추가된 글 수
        long addedBoardCommentCntTotal = 0; // resultBoards 에 board.getCommentCnt 총합
        long addedCommentCount = 0; // resultComments 에 추가된 댓글 수
        long addedDeletedCommentCount = 0; // resultComments 에 추가된 '삭제된 댓글'갯수

        try (Playwright playwright = Playwright.create()) {
            // 브라우저 기동
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setArgs(List.of("--no-sandbox", "--disable-setuid-sandbox", "--disable-gl-drawing-for-tests", "--blink-settings=imagesEnabled=false")));
            browserPage = browser.newPage();
            browserPage.setDefaultTimeout(2000);

            // 스크래핑 시작
            while (pageNum <= maxPageNum) {
                // 글 리스트 페이지 URL 설정
                String executeUrl = baseUrl + (isMinorGallery ? minorGalleryListUri : galleryListUri) + galleryIdParameterPrefix + galleryId + pageParameterPrefix + pageNum + listNumParameterPrefix + listNum;
                // 글 리스트 페이지 접속 및 파싱
                Elements trElements = openListPageAndParse(executeUrl);

                // 글 리스트 페이지 로드 실패
                if (trElements.isEmpty()) {
                    if (++retryCounter > maxRetryCount) {
                        throw new RetryExceededException("retryCounter exceeded, stop scraping, retryCounter = " + retryCounter + " Exception pageNum = " + pageNum);
                    }
                    continue; // 현재 페이지 재시도
                }
                retryCounter = 0; // 성공시 초기화

                // 글 리스트 페이지 갯수 누락시 재시도 (기다렷다가, 최대 3회)
                if (trElements.size() < Integer.parseInt(listNum) && trElementsReloadCounter < 3) {
                    log.warn("[SCRAPER] listContinue trElements.size = {}, pageNum = {} ", trElements.size(), pageNum);
                    trElementsReloadCounter++;
                    continue; // 현재 페이지 재시도
                }
                // 카운터 초기화는 실패리스트 추가 이후

                // 글 리스트 에서 글 마다 내용 추출
                List<String> hrefs = new ArrayList<>(); // 글의 href 저장할 리스트
                List<DcBoard> extractedBoards = new ArrayList<>(); // 글 리스트에서 추출한 DcBoard 리스트
                for (Element trElement : trElements) {
                    DcBoard extractingBoard = boardExtractor.extractFromListPage(trElement);
                    if (extractingBoard.getBoardNum() == -1) {
                        continue; // 공지, AD, 제휴글 등의 이유로 건너뛰어짐
                    } else {
                        // a 태그에서 글 href 추출 (/board/view/?id=granblue&no=4803525&page=1)
                        hrefs.add(trElement.select(boardHrefSelector).attr("href"));
                        extractedBoards.add(extractingBoard);
                    }
                }
                // 컷 카운터로 자르기
                if (cutCounter > 0) {
                    extractedBoards = extractedBoards.subList(0, (int) cutCounter);
                }
                resultBoards.addAll(extractedBoards);
                addedBoardCount += extractedBoards.size();

                // 리스트 url 3회이상 실패후 진행시 누락진행으로 실패리스트에 추가
                if (trElementsReloadCounter >= 3) {
                    log.warn("[BOARD.PROPERLY] boardNotAdded properly. listnum = {} addedBoardCount = {}, executeUrl = {}", listNum, addedBoardCount, executeUrl);
                    failures.add(new ScrapeFailure(executeUrl, trElements.size(), extractedBoards.size(), pageNum));
                }
                trElementsReloadCounter = 0; // 글 누락 리로드 카운터 초기화

                // 글 리스트에서 글 하나하나 순회시작
                int boardIndex = 0; // 글 순회 인덱스.
                while (boardIndex < extractedBoards.size()) {
                    // ScrapingOption.LISTPAGE 일 경우 중단
                    if (scrapingOption != ScrapingOption.ALL && scrapingOption != ScrapingOption.VIEWPAGE) break;
                    // 현재 글과 해당글의 href
                    DcBoard extractingBoard = extractedBoards.get(boardIndex);
                    String href = hrefs.get(boardIndex);
                    executeUrl = baseUrl + href;
                    // 글 상세 페이지 접속 및 파싱
                    Element mainElement = openViewPageAndParse(executeUrl);

                    // 글 상세 페이지 로드 실패
                    if (mainElement == null) {
                        if (++retryCounter > maxRetryCount) {
                            throw new RetryExceededException("retryCounter exceeded, stop scraping, retryCounter = " + retryCounter + " Exception pageNum = " + pageNum);
                        }
                        continue; // 현재 글 재시도
                    }
                    retryCounter = 0; // 성공시 초기화

                    // 상세 페이지에서 내용 추출 후 DcBoard 객체에 저장하여 완성
                    String rawContent = boardExtractor.extractContentFromViewPage(mainElement);
                    extractingBoard.setContent(rawContent);

                    // 완성 후 로깅
                    String cleanContent = ContentCleaner.cleanContent(rawContent);
                    log.debug("title = {} :: cleanContent = {}",
                            extractingBoard.getTitle(), cleanContent);
                    // 다음 글로
                    boardIndex++;

                    // ScrapingOption.VIEWPAGE 종료지점 ============================================
                    if (scrapingOption != ScrapingOption.ALL) { break; }

                    // 상세 페이지 댓글 추출 시작
                    List<DcComment> extractedComments = extractCommentsFromViewPage(extractingBoard.getBoardNum(), mainElement);
                    // 완성된 List<DcComment> 객체 저장
                    resultComments.addAll(extractedComments);

                    // '삭제된 댓글' 갯수 ( regDate = null )
                    int deletedCommentCount = extractedComments.stream().filter(dcComment -> dcComment.getRegDate() == null).toList().size();

                    addedCommentCount += extractedComments.size(); // resultComments 에 추가된 댓글 수
                    addedDeletedCommentCount += deletedCommentCount; // resultComments 에 추가된 '삭제된 댓글'갯수
                    addedBoardCommentCntTotal += extractingBoard.getCommentCnt(); // resultBoards 에 board.getCommentCnt 총합

                    // 댓글 개수 체크 (댓글돌이는 extractingBoard.getCommentCnt() 숫자에서 제외됨, 삭제된 댓글 갯수는 빼줘야됨)
                    if (extractingBoard.getCommentCnt() != extractedComments.size() - deletedCommentCount) {
                        log.warn("[COMMENT.PROPERLY] comment not extracted properly extractingBoard.getCommentCnt() = {} extractedComments.size() = {}, deletedCommentCount = {}, executeUrl={}", extractingBoard.getCommentCnt(), extractedComments.size(), deletedCommentCount, executeUrl);
                        extractedComments.forEach(dcComment -> log.debug("dcComment = {}", dcComment));
                    }

                    // 글 하나만 하고 끝내기
//                break;

                } // for trElements

                log.info("[PAGE END] pageNum = {}/{} resultBoards.size = {} resultComments.size = {}",
                        pageNum, maxPageNum, resultBoards.size(), resultComments.size());
                log.debug("addCommentCount = {}, addedBoardCommentCntTotal = {}, calculated expected = {}",
                        addedCommentCount, addedBoardCommentCntTotal, addedCommentCount - addedDeletedCommentCount);
                // 다음 페이지로
                pageNum++;

                // 카운터 초기화
                addedBoardCount = 0;
                addedCommentCount = 0;
                resultCommentCntFromBoard += addedBoardCommentCntTotal;
                addedBoardCommentCntTotal = 0;
                resultDeletedCommentCount += addedDeletedCommentCount;
                addedDeletedCommentCount = 0;

            }// for pageNum
        } // try with resouce
        return ScrapeResult.builder()
                .dcBoardsAndComments(
                        new DcBoardsAndComments(resultBoards, resultComments))
                .failure(failures)
                .loggingParams(
                        LoggingParams.builder()
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
     * 글 리스트 페이지를 열고, 파싱하여 글 리스트 Elements 를 반환함
     *
     * @param executeUrl
     * @return 글 리스트의 tr 요소로 구성된 Elements 객체, 실패시 size = 0 반환
     */
    protected Elements openListPageAndParse(String executeUrl) {
        Elements results = new Elements();
        try {
            log.debug("[Playwright] opening listPage executeUrl = {}", executeUrl);
            // 페이지 이동 및 요소탐색
            browserPage.navigate(executeUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT));
            ElementHandle gallList = browserPage.waitForSelector(boardListSelector);
            // 파싱
            String gallListOuterHtml = (String) gallList.evaluate("e => e.outerHTML");
            Element tableElement = Jsoup.parse(gallListOuterHtml);
            results = tableElement.select(boardListItemSelector);
        } catch (TimeoutError e) {
            log.debug("[TIMEOUT] openListPageAndParse();  executeUrl = {} e.name = {} message = {}", executeUrl, e.getClass().getName(), e.getMessage());
        } catch (Exception e) {
            log.debug("[ERROR] openListPageAndParse();  executeUrl = {} e.name = {} message = {}", executeUrl, e.getClass().getName(), e.getMessage());
        }
        return results;
    }

    /**
     * 글 상세 페이지를 열고, 파싱하여 본문 + 댓글 리스트를 포함하는 main 요소를 반환함
     *
     * @param executeUrl
     * @return 본문 + 댓글 리스트를 포함하는 main 요소 Element 객체, 실패시 null 반환
     */
    protected Element openViewPageAndParse(String executeUrl) {
        StopWatch stopwatch = new StopWatch(); // 개발용 스톱워치
        Element result; // 상세페이지의 main 요소 담길 변수
        try {
            log.debug("[Playwright] opening viewPage executeUrl = {}", executeUrl);
            stopwatch.start();
            // 페이지 이동 및 요소탐색
            browserPage.navigate(executeUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT));
            ElementHandle mainContainer = browserPage.waitForSelector(boardViewSelector, new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));
            // 파싱
            String mainContainerHtml = (String) mainContainer.evaluate("e => e.outerHTML");
            result = Jsoup.parse(mainContainerHtml);
            // 가끔 로드 느려서 .write_div = null 인경우 있어서 추가
            result = result.select(boardViewContentSelector).isEmpty() ? null : result;

            stopwatch.stop();
            log.debug("[STOPWATCH] get mainElement from page : stopwatch.elapsed = {}", stopwatch.getTotalTimeSeconds());
        } catch (TimeoutError e) {
            log.debug("[TIMEOUT] openViewPageAndParse();  executeUrl = {} e.name = {} message = {}", executeUrl, e.getClass().getName(), e.getMessage());
            result = null;
        } catch (Exception e) {
            log.debug("[ERROR] openViewPageAndParse();  executeUrl = {} e.name = {} message = {}", executeUrl, e.getClass().getName(), e.getMessage());
            result = null;
        }
        return result;
    }

    /**
     * 본문내용 + 댓글 요소를 포함하는 mainElement 로부터 댓글 리스트를 추출하여 반환
     *
     * @param mainElement
     * @param boardNum
     * @return
     */
    public List<DcComment> extractCommentsFromViewPage(long boardNum, Element mainElement) {
        List<DcComment> results = new ArrayList<>();

        // 댓글 리스트 ul 요소 추출
        Element ulElementComment = mainElement.select(commentListSelector).first();
        // 댓글 있으면 내용 추출
        if (ulElementComment != null) {
            // 댓글 리스트 내부 li 요소 (댓글 + 답글)
            Elements liElementsComment = ulElementComment.select(commentListItemSelector);
            Element liElementPrev = null; // 답글의 target 설정을 위한 직전 반복 댓글
            for (Element liElement : liElementsComment) {
                List<DcComment> extractedComments = commentExtractor.extractCommentAndReply(boardNum, liElement, liElementPrev);
                results.addAll(extractedComments);
                liElementPrev = liElement;
            }
        }
        return results;
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
        long boardPerPage = cutCounter == 0 ? Long.parseLong(listNum) : cutCounter;

        log.info("\n [SCRAPE COMPLETE] ==================================================\n" +
                        "  elaspedTime = {}h : {}m : {}s : {}millis, \n" +
                        "  time per board = {}s / board, per Page = {}s / page \n" +
                        "  startedFrom = {}, endTime = {}\n" +
                        "  page = {} ~ {} pageCount = {}\n " +
                        "  expectedBoardCounter = {} expectedCommentCounter = {} \n" +
                        "  scrapedBoardCounter = {}, scrapedCommentCounter = {}\n" +
                        " ======================================================================",
                duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart(),
                boardPerSecond, secondsPerPage,
                startTime, endTime,
                startPage, endPage, executePageCount,
                (executePageCount) * boardPerPage, scrapeStatus.getTotalDeletedCommentCnt() + scrapeStatus.getTotalCommentCntFromBoard(),
                scrapeStatus.getTotalBoardCnt(), scrapeStatus.getTotalCommentCnt());

        if (!failures.isEmpty()) {
            log.warn("\n[SCRAPE COMPLETE - FAILURES] failures.size = {} =====================================================", failures.size());
            failures.forEach(failure -> log.warn("failure = {}", failure));
            log.warn("\n=====================================================================================================");
        }
    }


    // 프로퍼티 설정 ==================================================
    private String baseUrl = "http://gall.dcinside.com";
    private String galleryListUri, minorGalleryListUri;
    private String galleryIdParameterPrefix;
    private String pageParameterPrefix;
    private String listNumParameterPrefix, listNum;
    private String searchParameterPrefix;
    private String boardListSelector, boardListItemSelector;
    private String boardHrefSelector;
    private String boardViewSelector;
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

        this.boardViewContentSelector = props.getBoardViewContentSelector();

        this.commentListSelector = props.getCommentListSelector();
        this.commentListItemSelector = props.getCommentListItemSelector();

        this.maxRetryCount = props.getMaxRetryCount();

    }
}

/*
디시 글 갯수
페이지당 50개, 1페이지의 경우 공지글을 포함하여 50개.
그랑블루갤러리의 경우 현재 공지 2개
48 + 50 * N
 */


