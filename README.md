# DC scraper
## 개요
국내 커뮤니티 디시 인사이드의 글을 스크래핑 하는 스크래퍼 입니다.

## 1. 라이브러리 추가
repositories와 dependencies에 아래 코드를 추가합니다.
```build.gradle
repositories {
    //...
    maven { url 'https://jitpack.io' } 
}
dependencies {
    //...
    implementation 'com.github.spiamint:DcScraper:0.0.13' 
}
```

## 2. 스크래퍼 사용 예시
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

#### 1.2 콜백을 실행하는 스크래핑
```java
    @Autowired
    private DcScraper dcScraper;

    public void test2() {
        dcScraper.startWithCallback(ScrapeRequest.of(
                "github", true, 1, 1, 1), // 갤러리ID, 마이너 갤리러 여부, 시작페이지, 끝페이지, 콜백 인터벌
                this::callBack);  // 실행 할 콜백
    }
    public void callBack(DcBoardsAndComments scraped) {
        scraped.getBoards().forEach(dcBoard -> log.info(dcBoard.cleanedToString())); // 스크래핑 된 글
        scraped.getComments().forEach(dcComment -> log.info(dcComment.cleanedToString())); // 스크래핑 된 댓글
    }
```

#### 1.3 스크래핑 설정
