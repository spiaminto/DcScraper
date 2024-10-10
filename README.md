# DC scraper
## 개요
국내 커뮤니티 디시 인사이드의 글을 스크래핑 하는 스크래퍼 입니다.

## 1. 의존성 추가
build.gradle 또는 pom.xml 파일의 repositories 와 dependencies 에 아래와 같이 의존성을 추가합니다.  

build.gradle
```gradle
repositories {
    maven { url 'https://jitpack.io' } 
}

dependencies {
    implementation 'com.github.spiamint:DcScraper:1.0.8' 
}
```
pom.xml
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
    <version>1.0.8</version>
</dependency>
```

첫 스크래핑 실행 시 Microsoft Playwright 라이브러리가 Chromium 브라우저를 'C:\Users\[사용자명]\AppData\Local\ms-playwright' 에 다운로드 합니다.

## 2. 스크래퍼
#### 1.1 기본 스크래핑
```java
    @Autowired
    private DcScraper dcScraper;

    public void startTest() {
        DcBoardsAndComments scraped = dcScraper.start(
                ScrapeRequest.of("github", GalleryType.MINOR, 1, 1)); // 갤러리ID, 갤러리 타입, 시작페이지, 끝페이지
        scraped.getBoards().forEach(dcBoard -> log.info(dcBoard.cleanedToString())); // 스크래핑 된 글
        // DcBoard(boardNum=71100, title=c언어 강의 추천하는거 있나요?, cleanContent=예시문제 같은것도 있었음 좋겠는데 추천좀 해주세요, writer=거북이이, regDate=2024-10-07T18:50:17, viewCnt=36, commentCnt=4, recommendCnt=0, recommended=false)
        
        scraped.getComments().forEach(dcComment -> log.info(dcComment.cleanedToString())); // 스크래핑 된 댓글
        // DcComment(commentNum=394827, boardNum=71100, writer=거북이이, cleanContent=오 좋아보이네요 ㄱㅅㄱㅅ, regDate=2024-10-07T19:11:22, reply=true, targetNum=394826)
    }
```
DcScraper 객체를 주입받아 시작 페이지 부터 끝 페이지 까지 글과 댓글 모두를 스크래핑 합니다.  
한 페이지당 글 갯수의 기본값은 100개 이며 설문, 공지 글은 스크래핑 되지 않습니다.  
  
갤러리ID 는 디시인사이드 url의 id 파라미터(?id=github) 값입니다.    
  
갤러리 타입은 MAJOR(정규 갤러리), MINOR(마이너 갤러리) 두가지가 있습니다.  
  
DcBoard 의 content 필드는 html 태그가 포함된 문자열입니다. getCleanContent(), cleanedToString() 메소드를 사용하여 태그를 제거한 문자열을 얻을 수 있습니다.

#### 1.2 콜백을 실행하는 스크래핑
```java
    @Autowired
    DcScraper dcScraper;

    public void callbackTest() {
        dcScraper.startWithCallback(
                ScrapeRequest.of("github", GalleryType.MINOR, 1, 3, 2), // 갤러리ID, 갤러리 타입, 시작페이지, 끝페이지, 콜백 인터벌
                this::callBack);  // 실행 할 콜백
    }
    public void callBack(DcBoardsAndComments scraped) {
        scraped.getBoards().forEach(dcBoard -> log.info(dcBoard.cleanedToString())); // 스크래핑 된 글
        scraped.getComments().forEach(dcComment -> log.info(dcComment.cleanedToString())); // 스크래핑 된 댓글
    }
```
주어진 콜백 인터벌 마다 스크래핑 결과인 DcBoardsAndComments 객체를 전달하여 비동기 방식으로 콜백을 실행합니다.  
위의 예시에서, 1페이지 - 2페이지 - 콜백(1,2) - 3페이지 - 콜백(3) 순서로 실행됩니다.

#### 1.3 스크래핑 설정
```java
    @Autowired
    DcScraper dcScraper;
    @Autowired @Qualifier("ThreadPoolTaskExecutor")
    Executor executor;

    public void optionTest() {
        dcScraper.setCutCounter(5); // 한 리스트 페이지에서 스크래핑할 글 갯수 제한
        dcScraper.setScrapingOption(ScrapingOption.VIEWPAGE); // 스크래핑 옵션(범위) 설정
        dcScraper.setMaxRetryCount(5); // 최대 재시도 횟수 설정
        dcScraper.setExecutor(executor); // 콜백 실행에 사용할 Executor 설정
        dcScraper.startWithCallback(
                ScrapeRequest.of("github", GalleryType.MINOR, 1, 1, 1),
                this::callBack);
        // boards size: 5, comments size: 0
    }
    public void callback(DcBoardsAndComments scraped) {
        log.info("boards size: " + scraped.getBoards().size());
        log.info("comments size: " + scraped.getComments().size());
    }
```
스크래핑 시 위와 같이 설정을 변경할 수 있습니다.  
+ ScrapingOption 스크래핑 옵션
  + ALL: 리스트페이지와 상세페이지에 모두 접속하여 글 과 댓글 모두 스크래핑 (기본값)
  + VIEWPAGE: 리스트페이지와 상세페이지 모두 접속하여 글 만 스크래핑
  + LISTPAGE: 리스트페이지만 접속하여 글(내용없음) 만 스크래핑 

## 3. 페이지 파인더 
```java
    @Autowired
    DcPageFinder pageFinder;

    public void findPageTest() {
        pageFinder.findPage(LocalDate.of(2024, 01, 01), "github", GalleryType.MINOR); // 2024년 1월 1일의 github 마이너 갤러리 페이지를 찾음
        // [SEARCH] End ==============================================
        // find page number = 113
        // -> check URL = http://gall.dcinside.com/mgallery/board/lists/?id=github&page=113&list_num=100
    }
```
특정 날짜를 기준으로 스크래핑 하고 싶을때, 해당 날짜의 글이 있는 페이지를 찾아주는 기능을 제공합니다.  
한 페이지 정도 오차가 있을 수 있기 때문에 로그를 확인하여 찾은 페이지가 맞는지 확인해야 합니다.

## 4. 프로퍼티 설정
```properties
# 표시 글 갯수 설정 (url 파라미터의 list_num 값 설정)
scraper.list-num=50
# 글 번호 추출시 사용할 css 선택자 설정
board-extractor.board-num-selector=#selector
# 글 번호 추출시 사용할 html 속성 설정 (text, innerHTML, title, data-no 등)
board-extractor.board-num-attr=text
```
```java
    @Autowired
    DcScraper scraper;

    public void propertyTest(){
        scraper.start(ScrapeRequest.of("github",GalleryType.MINOR,1,1)); // 접속 url = http://gall.dcinside.com/mgallery/board/lists/?id=github&page=1&list_num=50
    }
```
요소를 찾을 선택자, 속성, 스크래핑 url 등을 설정할 수 있습니다.  
각 프로퍼티의 접두어는 scraper, board-extractor, comment-extractor, page-finder 입니다.  
scraper.list-num 이외의 속성은 필요한 경우에만 한해 사용하기를 권장합니다.

## 5. 사용 라이브러리
+ Spring Boot
+ Playwright
+ Jsoup
+ Lombok
