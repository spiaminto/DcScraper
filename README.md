# DC scraper
## 개요
국내 커뮤니티 디시 인사이드의 글을 스크래핑 하는 스크래퍼 입니다.

## 1. 라이브러리 추가
repositories와 dependencies에 아래 코드를 추가합니다.
```gradle
repositories {
    //...
    maven { url 'https://jitpack.io' } 
}
dependencies {
    //...
    implementation 'com.github.spiamint:DcScraper:1.0.6' 
}
```
```maven
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>com.github.spiamint</groupId>
    <artifactId>DcScraper</artifactId>
    <version>1.0.6</version>
</dependency>
```

## 2. 스크래퍼
#### 1.1 기본 스크래핑
```java
    @Autowired
    private DcScraper dcScraper;

    public void startTest() {
        DcBoardsAndComments scraped = dcScraper.start(ScrapeRequest.of(
                "github", true, 1, 1)); // 갤러리ID, 마이너 갤리러 여부, 시작페이지, 끝페이지

        scraped.getBoards().forEach(dcBoard -> log.info(dcBoard.cleanedToString()));
        // DcBoard(dcNum=71100, title=c언어 강의 추천하는거 있나요?, cleanContent=예시문제 같은것도 있었음 좋겠는데 추천좀 해주세요, writer=거북이이, regDate=2024-10-07T18:50:17, viewCnt=36, commentCnt=4, recommendCnt=0, recommended=false)
        
        scraped.getComments().forEach(dcComment -> log.info(dcComment.cleanedToString())); // 스크래핑 된 댓글
        // DcComment(commentNum=394827, boardNum=71100, writer=거북이이, cleanContent=오 좋아보이네요 ㄱㅅㄱㅅ, regDate=2024-10-07T19:11:22, reply=true, targetNum=394826)
    }
```
기본적으로 시작 페이지 부터 끝 페이지 까지 글과 댓글 모두를 스크래핑 합니다. 페이지 당 글 갯수는 100개 입니다.
설문, 공지 글은 스크래핑 되지 않습니다.

#### 1.2 콜백을 실행하는 스크래핑
```java
    @Autowired
    private DcScraper dcScraper;

    public void callbackTest() {
        dcScraper.startWithCallback(ScrapeRequest.of(
                "github", true, 1, 3, 2), // 갤러리ID, 마이너 갤리러 여부, 시작페이지, 끝페이지, 콜백 인터벌
                this::callBack);  // 실행 할 콜백
    }
    public void callBack(DcBoardsAndComments scraped) {
        scraped.getBoards().forEach(dcBoard -> log.info(dcBoard.cleanedToString())); // 스크래핑 된 글
        scraped.getComments().forEach(dcComment -> log.info(dcComment.cleanedToString())); // 스크래핑 된 댓글
    }
```
주어진 콜백 인터벌 마다 콜백을 실행하여 스크래핑 결과를 전달합니다.  
위의 예시에서, 1 - 2 - 콜백 - 3 - 콜백 순으로 실행됩니다.

#### 1.3 스크래핑 설정
```java
    @Autowired
    private DcScraper dcScraper;

    public void optionTest() {
        dcScraper.setCutCounter(5); // 한 리스트 페이지에서 스크래핑할 글 갯수 제한
        dcScraper.setScrapingOption(ScrapingOption.VIEWPAGE); // 스크래핑 옵션(범위) 설정
        dcScraper.setMaxRetryCount(5); // 최대 재시도 횟수 설정
        DcBoardsAndComments scraped = dcScraper.start(ScrapeRequest.of(
                "github", true, 1, 1));
        log.info("Total boards: " + scraped.getBoards().size()); // 5
        log.info("Total comments: " + scraped.getComments().size()); // 0
    }
```
+ ScrapingOption 스크래핑 옵션
  + ALL: 리스트페이지와 상세페이지에 모두 접속하여 글 과 댓글 모두 스크래핑 (기본값)
  + VIEWPAGE: 리스트페이지와 상세페이지 모두 접속하여 글 만 스크래핑
  + LISTPAGE: 리스트페이지만 접속하여 글(내용없음) 만 스크래핑 

## 3. 페이지 파인더 
```java
@Autowired
private DcPageFinder dcPageFinder;

public void findPageTest() {
        pageFinder.findPage(LocalDate.of(2024, 01, 01), "github", true); // 2024년 1월 1일의 github 마이너 갤러리 페이지를 찾음
    }
```
특정 날짜를 기준으로 스크래핑 하고 싶을때, 해당 날짜의 글이 있는 페이지를 찾아주는 기능을 제공합니다.  
한 페이지 정도 오차가 있을 수 있기 때문에 로그를 확인하여 찾은 페이지가 맞는지 확인해야 합니다.

## 4. 프로퍼티 설정
```properties
# 표시 글 갯수 설정 (url 파라미터의 list_num 값 설정)
scraper.list-num=50
```
```java
    @Autowired
    private DcPageFinder dcPageFinder;

    public void propertyTest(){
        dcScraper.start(ScrapeRequest.of("github",true,1,1)); // 접속 url = http://gall.dcinside.com/mgallery/board/lists/?id=github&page=1&list_num=50
    }
```
요소를 찾을 선택자, 속성, 스크래핑 url 등을 설정할 수 있습니다.  
각 프로퍼티의 접두어는 scraper, board-extractor, comment-extractor, page-finder 입니다.  
scraper.list-num 이외의 속성은 필요한 경우에만 사용하기를 권장합니다.

## 4. 사용 라이브러리
+ Spring Boot
+ Playwright
+ Jsoup
+ Lombok
