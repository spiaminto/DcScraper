package io.github.spiaminto.dcscraper.service.impl;


import com.microsoft.playwright.*;
import io.github.spiaminto.dcscraper.dto.DcBoard;
import io.github.spiaminto.dcscraper.exception.RetryExceededException;
import io.github.spiaminto.dcscraper.extractor.BoardExtractor;
import io.github.spiaminto.dcscraper.properties.PageFinderProperties;
import io.github.spiaminto.dcscraper.service.DcPageFinder;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StopWatch;

import java.net.URLEncoder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class DefaultDcPageFinder implements DcPageFinder {
    private final BoardExtractor boardExtractor;
    private Page browserPage;

    private final String baseUrl;
    private final String galleryListUri;
    private final String minorGalleryListUri;
    private final String galleryNameParameterPrefix; // 반드시 첫번째로 사용하는 갤러리 이름 파라미터
    private final String pageParameter; // 페이징 파라미터
    private final String searchParameter; // 검색 파라미터(제목+내용)
    private final String listNumParameter; // 리스트 갯수 파라미터

    private final long maxRetryCount;

    public DefaultDcPageFinder(BoardExtractor boardExtractor, PageFinderProperties pageFinderProperties) {
        this.boardExtractor = boardExtractor;
        maxRetryCount = 3;
        // 프로퍼티
        this.baseUrl = pageFinderProperties.getBaseUrl();
        this.galleryListUri = pageFinderProperties.getGalleryListUri();
        this.minorGalleryListUri = pageFinderProperties.getMinorGalleryListUri();
        this.galleryNameParameterPrefix = pageFinderProperties.getGalleryNameParameterPrefix();
        this.pageParameter = pageFinderProperties.getPageParameter();
        this.searchParameter = pageFinderProperties.getSearchParameter();
        this.listNumParameter = "&list_num=100";

    }

    public void findPage(LocalDate inputDate, String galleryId, boolean isMinorGallery) {
        // 드라이버 켜기
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setArgs(List.of(
                                    "--no-sandbox",
                                    "--disable-setuid-sandbox",
                                    "--disable-gl-drawing-for-tests",
                                    "--blink-settings=imagesEnabled=false"
                            )
                    ));
            browserPage = browser.newPage();
            browserPage.setDefaultTimeout(5000);

            // 검색 페이지로 이동을 위한 url 설정
            String searchKeyword = "p"; // 글 내부에 p 요소 있으면 전부 검색됨 (div 등으로 확인)
            String encodedKeyword = URLEncoder.encode(searchKeyword);
            String galleryUri = isMinorGallery ? minorGalleryListUri : galleryListUri;
            String urlPreFix = baseUrl + galleryUri + galleryNameParameterPrefix + galleryId;

            String executeUrl = urlPreFix + searchParameter + encodedKeyword;

            // 검색할 날짜의 년, 월, 일을 각각 문자열로 변환, 0붙임, inputDate 의 자정을 targetDateTime 으로 지정
            String inputYear = "" + inputDate.getYear();
            String inputMonth = inputDate.getMonthValue() > 10 ? "" + inputDate.getMonthValue() : "0" + inputDate.getMonthValue();
            String inputDay = inputDate.getDayOfMonth() > 10 ? "" + inputDate.getDayOfMonth() : "0" + inputDate.getDayOfMonth();
            LocalDateTime targetDateTime = inputDate.atStartOfDay();

            long retryCounter = 0;
            while (retryCounter < maxRetryCount) {

                StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                try {

                    // 검색 페이지 접속
                    log.info("[PageFinder] findPage Initial executeUrl = {}", executeUrl);
                    browserPage.navigate(executeUrl);
                    log.info("[PageFinder] moving to target time page");

                    // 빠른 이동 버튼 클릭
                    ElementHandle moveButton = browserPage.waitForSelector(".bottom_movebox>button");
                    moveButton.click();
                    log.debug("빠른이동 버튼 클릭");

                    // 작성일 입력 클릭
                    ElementHandle calendarInput = browserPage.waitForSelector(".moveset #calendarInput");
                    calendarInput.click();
                    log.debug("작성일 입력 클릭");

                    // 이전달 버튼의 onclick 메서드 수정후 클릭
                    ElementHandle prevMonthBtn = browserPage.waitForSelector(".btn_prev_month");
                    prevMonthBtn.evaluate("e => e.setAttribute('onclick', 'openCalendar(" + inputMonth + "," + inputYear + ")')");
                    prevMonthBtn.click();
                    log.debug("이전 달 버튼 클릭");

                    // 날짜 박스 클릭
                    String inputDateTimeString = inputYear + "-" + inputMonth + "-" + inputDay;
                    ElementHandle dayBtn = browserPage.waitForSelector("#calendar_day td[data-day='" + inputDateTimeString + "']");
                    dayBtn.click();
                    log.debug("달력의 날짜 버튼 클릭");

                    // 빠른이동 내부 확인버튼 클릭하여 페이지 이동
                    browserPage.waitForSelector(".fast_move_btn").click();
                    log.debug("작성일 검색 페이지 이동, 현재페이지 = {}", browserPage.url());

                    // 디시에서 빠른이동 검색결과 없음 알림 뜬 경우 break
                    try {
                        ElementHandle noBoardInDayErrorElement = browserPage.waitForSelector(".hint_txt.font_red");
                        String errorMessageFromDc = noBoardInDayErrorElement.textContent();
                        log.error("해당 일에 작성된 글 없음. DC 알림내용 = {}", errorMessageFromDc);
                        break;
                    } catch (Exception e) {
                        log.debug("해당 일에 작성된 글 있음. 정상진행");
                    }

                    // 타겟 날짜의 페이지 도달 함
                    ElementHandle targetPageGallList = browserPage.waitForSelector(".gall_list");
                    // 타겟 날짜의 페이지에서 리스트중 첫번째 글 파싱 및 dcNum 추출
                    DcBoard targetDcBoard = parseAndFindFirstDcBoard(targetPageGallList);
                    Long targetDcNum = targetDcBoard.getBoardNum();
                    log.debug("해당 일에 작성된 마지막 글 추출");

                    // 갤러리 첫 페이지로 이동 및 첫 표시 글 dcNum 파싱 및 추출
                    executeUrl = urlPreFix + pageParameter + 1;
                    browserPage.navigate(executeUrl);
                    ElementHandle firstPageGallList = browserPage.waitForSelector(".gall_list");
                    DcBoard firstDcBoard = parseAndFindFirstDcBoard(firstPageGallList);
                    Long firstPageDcNum = firstDcBoard.getBoardNum();
                    log.debug("갤러리 맨 첫페이지로 이동 및 최신 글 추출");

                    // 디시 아이디의 차를 구하여 표시 글 갯수(50개) 로 나눔
                    // 이 lastPage 는 삭제된 글이 0 이라고 가정한 수치이므로 이로부터 삭제된 글을 감안하여 페이지를 당겨가며 확인
                    Long dcNumDiff = firstPageDcNum - targetDcNum;
                    Long lastPage = dcNumDiff / 100;

                    // 1. 먼저 lastPage 부터 1000페이지씩 당겨 inputTime 보다 미래 글 이 포함된 페이지를 찾는다.
                    // 2. 해당 페이지를 searchStartPage 로 두고, 직적 페이지를 searchEndPage 로 둔다. 둘의 차는 1000페이지
                    // 3. inputTime 의 글은 두 페이지 사이에 있다.

                    log.info("[PageFinder] finding exact Page");
                    LocalDateTime lastPageFirstDcBoardTime;
                    long searchEndPage = lastPage; // 밑에 탐색에서 끝페이지로 사용할 페이지. (가장 과거)
                    int lastPageWhileIndex = 0;
                    do {
                        searchEndPage = lastPage;
                        lastPage -= 1000;
                        // 끝페이지의 첫글을 찾아 작성일자 대조 후 targetTime 보다 과거글이면 1000페이지씩 당김
                        executeUrl = urlPreFix + pageParameter + lastPage + listNumParameter;
                        browserPage.navigate(executeUrl);
                        ElementHandle lastPageGallList = browserPage.waitForSelector(".gall_list");
                        DcBoard lastPageFirstDcBoard = parseAndFindFirstDcBoard(lastPageGallList);
                        lastPageFirstDcBoardTime = lastPageFirstDcBoard.getRegDate();
                        log.debug("페이지 당기는 중, 현재페이지 = {}", lastPage);
                        if (lastPage < 0) {
                            lastPage = 1L;
                            break;
                        } // 페이지 당기는데 음수되면 1페이지로 조정 및 종료
                    } while (lastPageFirstDcBoardTime.isBefore(targetDateTime)); // 당겨온 페이지의 첫글이 inputDateTime 보다 미래 글이면 stop
                    long searchStartPage = lastPage; // 밑의 탐색에서 시작 페이지로 사용할 페이지. (가장 미래)
                    log.debug("탐색을 위한 페이지 찾기 완료. 탐색 시작 페이지 = {}, 탐색 끝 페이지 = {}", searchStartPage, searchEndPage);

                    // 글은 lastPage 와 prevPage 사이에 있음
                    long middlePage = (searchStartPage + searchEndPage) / 2;
                    long prevMiddelPage = 0; // 중복 탐색을 방지하기 위한 직전 middlePage

                    String currentUrl;
                    int index = 0;
                    LocalDateTime middlePageFirstDcBoardTime = null;
                    log.debug("작성일 페이지 탐색 시작");
                    // 이진탐색 (최대 10회)
                    while (index < 10) {
                        log.debug("작성일 페이지 탐색중 {}/10", index + 1);
                        // 중간 페이지 계산
                        middlePage = (searchStartPage + searchEndPage) / 2;

                        if (middlePage == prevMiddelPage) { // 동일 페이지 재탐색 break
                            log.debug("동일 페이지 탐색시도 에 따른 탐색 종료. 결과페이지 = {}", middlePage);
                            break;
                        }

                        // 중간 페이지 접속
                        executeUrl = urlPreFix + pageParameter + middlePage + listNumParameter;
//                    log.info("binarySearching else executeUrl = {}", executeUrl);
                        browserPage.navigate(executeUrl);

                        // 중간 페이지 최상단 글 추출
                        ElementHandle middlePageGallList = browserPage.waitForSelector(".gall_list");
                        DcBoard middlePageDcBoard = parseAndFindFirstDcBoard(middlePageGallList);
                        middlePageFirstDcBoardTime = middlePageDcBoard.getRegDate();

                        if (Duration.between(middlePageFirstDcBoardTime, targetDateTime).abs().toMinutes() <= 1) {
                            //  inputTime 과의 차가 1분 이하 이면 break
                            break;
                        } else {
                            // 중간 페이지와 inputTime 의 차가 1분 이내가 아님
                            log.debug("Searching else searchStartPage = {}, searchEndPage = {}, middlePage = {}, middlePageFirstDcBoardTime = {} executeUrl = {}",
                                    searchStartPage, searchEndPage, middlePage, middlePageFirstDcBoardTime, executeUrl);
                            prevMiddelPage = middlePage; // 중복 탐색 방지를 위한 이전 middlePage 저장

                            if (middlePageFirstDcBoardTime.isAfter(targetDateTime)) {
                                // 중간 페이지 첫글 이 inputTime 보다 미래
                                searchStartPage = middlePage;
                            } else {
                                // 중간 페이지 첫글 이 inputTime 보다 미래
                                searchEndPage = middlePage;
                            }
                        }
                        index++;
                    }

                    // 성공 종료 로깅
                    log.info("\n[SEARCH] End ==============================================\n" +
                            "find page number = {}\n" +
                            " -> check URL = {}\n",
                            middlePage, executeUrl);
                    log.debug("\nindex = {}\n" +
                                    "Duration minute = {}m middlePageFirstDcBoardTime = {}, inputDateTime = {}\n" +
                                    "searchStartPage = {} searchEndPage = {}\n" +
                                    "middlePage = {} confirmUrl = {}\n" +
                                    "[BINARYSEARCH] End =================================",
                            index,
                            Duration.between(middlePageFirstDcBoardTime, targetDateTime).abs().toMinutes(), middlePageFirstDcBoardTime, targetDateTime,
                            searchStartPage, searchEndPage, middlePage,
                            executeUrl);

                    stopWatch.stop();
                    log.debug("[STOPWATCH] findFirstPageByDate : stopwatch.elapsed = {}", stopWatch.getTotalTimeSeconds());

                    // 성공시 바로 종료
                    break;

//                } catch (ElementClickInterceptedException e) {
                    //TODO 수정필요
                    // 마이너 갤러리 알림 팝업 발생시 닫고 재시도
//                    log.error("[ERROR] ElementClickInterceptedException : {}", e.getMessage());
//                    closeMinorGalleryPopup(executeUrl);
//                    retryCounter++;
//                     재시도 횟수 초과 -> Exception 발생 및 스크래핑 종료
//                    if (retryCounter > maxRetryCount) {
//                        throw new RetryExceededException("retryCounter exceeded, stop scraping, retryCounter = {}" + retryCounter, e);
//                    }
                } catch (Exception e) {
                    log.error("[ERROR] executeUrl = {} e.name = {} message = {}", executeUrl, e.getClass().getName(), e.getMessage());
                    retryCounter++;
                    // 재시도 횟수 초과 -> Exception 발생 및 스크래핑 종료
                    if (retryCounter > maxRetryCount) {
                        throw new RetryExceededException("retryCounter exceeded, stop scraping, retryCounter = {}" + retryCounter, e);
                    }
                } finally {
                }
            }
        }
    }


    public DcBoard parseAndFindFirstDcBoard(ElementHandle gallListElement) {
        Elements trElements = Jsoup.parse((String) gallListElement.evaluate("e => e.outerHTML")).select("tbody tr");
        DcBoard result = null;
        for (Element trElement : trElements) {
            DcBoard extractedBoard = boardExtractor.extractFromListPage(trElement);
            if (extractedBoard.getBoardNum() != -1) {
                result = extractedBoard;
                break;

            }
        }
        return result;
    }

    public void closeMinorGalleryPopup(String executeUrl) {
        browserPage.navigate(executeUrl);
        browserPage.waitForSelector("#closure-popup .btn_bottom.fl").click();
    }


}
