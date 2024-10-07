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
    implementation 'com.github.spiamint:DcScraper:0.0.13' 
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
    <version>0.0.15</version>
</dependency>
```

## 2. 스크래퍼
#### 1.1 기본 스크래핑
```java
    @Autowired
    private DcScraper dcScraper;

    public void test() {
        DcBoardsAndComments scraped = dcScraper.start(ScrapeRequest.of(
                "github", true, 1, 1)); // 갤러리ID, 마이너 갤리러 여부, 시작페이지, 끝페이지
        scraped.getBoards().forEach(dcBoard -> log.info(dcBoard.cleanedToString())); // 스크래핑 된 글
        scraped.getComments().forEach(dcComment -> log.info(dcComment.cleanedToString())); // 스크래핑 된 댓글
    }
```
기본적으로 시작 페이지 부터 끝 페이지 까지 글과 댓글 모두를 스크래핑 합니다. 페이지 당 글 갯수는 100개 입니다.

#### 1.2 콜백을 실행하는 스크래핑
```java
    @Autowired
    private DcScraper dcScraper;

    public void test2() {
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

    public void test3() {
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
특정 날짜를 기준으로 스크래핑 하고 싶을때, 해당 날짜의 글이 있는 페이지를 찾아주는 기능을 제공합니다.
```java
    @Autowired
    private DcPageFinder dcPageFinder;

    public void test4() {
        int page = dcPageFinder.findPageByDate("github", true, LocalDate.of(2021, 1, 1)); // 갤러리ID, 마이너 갤러리 여부, 찾을 날짜
        log.info("Page: " + page); // 1
    }
```
