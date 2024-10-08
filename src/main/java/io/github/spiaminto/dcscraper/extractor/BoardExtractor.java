package io.github.spiaminto.dcscraper.extractor;

import io.github.spiaminto.dcscraper.dto.DcBoard;
import io.github.spiaminto.dcscraper.properties.BoardExtractorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoardExtractor {

    protected final BoardExtractorProperties props;

    public long parseGallNum(String gallNum) {
        try {
            return Long.parseLong(gallNum); // gallNum 파싱하여 반환
        } catch (Exception e) { // 설문, 공지 AD 등 일경우 gallNum 이 없어 NumberFormatException 발생
//            log.warn("[WARN] gallNum = {} is not number", gallNum);
            return -1;
        }
    }

    /**
     * 마이너 갤러리에서 일반글 여부 판단
     * @param trElement
     * @return
     */
    public boolean extractableMinorGallery(Element trElement) {
        return trElement.hasClass("us-post") && !trElement.select(".gall_subject b").text().equals("공지");
    }

    /**
     * 디시 글 리스트(테이블) 의 tr 요소를 받아 필요 내용을 추출
     *
     * @param trElement
     * @return content, recommend 가 set 되지 않은 DcBoard 객체. 공지나 AD 등 추출 실패시 boardNum = -1 인 DcBoard 객체 반환
     */
    public DcBoard extractFromListPage(Element trElement) {
        String gallNumString = selectBy(trElement, props.getBoardNumSelector(), props.getBoardNumAttr());
        if (!extractableMinorGallery(trElement)) return DcBoard.builder().boardNum(-1L).build();
        long gallNum = parseGallNum(gallNumString);
        if (gallNum == -1) {
            return DcBoard.builder().boardNum(-1L).build();
        }

        // 내용 추출
        String title = selectBy(trElement, props.getTitleSelector(), props.getTitleAttr());
        String writer = selectBy(trElement, props.getWriterSelector(), props.getWriterAttr());
        String regDate = selectBy(trElement, props.getRegDateSelector(), props.getRegDateAttr());
        String viewCnt = selectBy(trElement, props.getViewCntSelector(), props.getViewCntAttr());
        String recommendCnt = selectBy(trElement, props.getRecommendCntSelector(), props.getRecommendCntAttr());
        String commentCnt = selectBy(trElement, props.getCommentCntSelector(), null);
        String recommended = selectBy(trElement, props.getRecommendSelector(), null); // 글 아이콘 이미지에 .icon_recomimg 가 있으면 개념글
        boolean isRecommended = recommended.equals("true");

        // 날짜 변환 형식 : 2024-07-19 12:05:43
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime convertedRegDate = LocalDateTime.parse(regDate, formatter);

        // 글 저장용 객체 생성(내용제외)
        return DcBoard.builder()
                .boardNum(gallNum)
                .title(title)
                .writer(writer)
                .regDate(convertedRegDate)
                .viewCnt(Long.parseLong(viewCnt))
                .recommendCnt(Long.parseLong(recommendCnt))
                .commentCnt(Long.parseLong(commentCnt))
                .recommended(isRecommended)
                .build(); // content, recommend null
    }

    /**
     * 디시 글 상세 페이지에서 글 내용 추출
     * @param mainElement
     * @return 추출된 글 내용 (String html)
     */
    public String extractContentFromViewPage(Element mainElement) {
        // 내용 추출
        String rawContent = selectBy(mainElement, props.getContentSelector(), props.getContentAttr());

        // 글 내용에서 념글 확인
//        String recommended = selectBy(mainElement, recommendSelector, null); // 념글버튼의 클래스에 .on 붙으면 념글
//        boolean isRecommended = recommended.equals("true");

        // DcBoard 객체에 내용 추가
//        dcBoard.setRecommended(isRecommended);
//        dcBoard.setContent(rawContent);

//        String cleanContent = ContentCleaner.cleanContent(rawContent);
//        log.info("\n================================================================\n" +
//                        " title = \n" +
//                        "   {}\n" +
//                        " cleanContent = \n" +
//                        "   {}\n" +
//                        "================================================================\n",
//                dcBoard.getTitle(),
//                cleanContent
//        );

        return rawContent;
    }


    protected String selectBy(Element trElement, String selector, String attr) {
        try {
            if (selector.equals(props.getCommentCntSelector())) { // commentCnt 는 별도추출
                return extractCommentCnt(trElement).toString();
            }
            if (selector.equals(props.getRecommendSelector())) { // recommend 는 selector 로 empty 면 false
                return trElement.select(selector).isEmpty() ? "false" : "true";
            }

            if (attr.equals("text")) {
                return trElement.select(selector).first().text();
            } else if (attr.equals("innerHtml")) {
                return trElement.select(selector).first().html();
            } else if (attr.equals("outerHtml")) {
                return trElement.select(selector).first().outerHtml();
            } else {
                return trElement.select(selector).first().attr(attr);
            }
        } catch (Exception e) { // 에러난 요소 확인용 catch
            log.error("[BOARD EXTRACTOR] selectBy " +
                    "element = \n" +
                    "{}\n" +
                    " selector = \n" +
                    "  {}\n " +
                    " attr = \n" +
                    "  {}\n" +
                    " error: \n{}\n" +
                    " errorMessage: \n{}", trElement, selector, attr, e, e.getMessage());
            throw e; // 에러 처리 X
        }
    }

    /**
     * 댓글 수 추출. 이 작업은 디시의 표현 방식이 바뀌면 어그러질 가능성이 높기떄문에 text() 로 고정.
     * @param trElement
     * @return
     */
    protected Long extractCommentCnt(Element trElement) {
        long result;
        Element commentCntElement = trElement.select(props.getCommentCntSelector()).first();
        String commentCnt = commentCntElement != null ? // 댓글 없으면 null 임
                commentCntElement.text().replaceAll("[\\[\\]]", "") : // text = [1]
                "0";
        int slashIndex = commentCnt.indexOf("/");
        if (slashIndex > -1) { // 보이스 리플 있으면 8/1 이렇게 표시됨
            String normalCommentCnt = commentCnt.substring(0, slashIndex);
            String voiceCommentCnt = commentCnt.substring(slashIndex + 1);
            result = Long.parseLong(normalCommentCnt) + Long.parseLong(voiceCommentCnt);
        } else {
            result = Long.parseLong(commentCnt);
        }
        return result;
    }

}


/*
DC 스크래핑 후 글 형태
select(.ub-content)
    <tr class="ub-content" data-no="1" data-type="icon_notice">
     <td class="gall_num">공지</td>
     <td class="gall_tit ub-word"><a href="/board/view/?id=granblue&amp;no=1&amp;page=1" view-msg=""> <em class="icon_img icon_notice"></em><b><b>그랑블루 판타지 갤러리 이용 안내</b></b></a> <a class="reply_numbox" href="https://gall.dcinside.com/board/view/?id=granblue&amp;no=1&amp;t=cv&amp;page=1"><span class="reply_num">[124]</span></a></td>
     <td class="gall_writer ub-writer" data-nick="운영자" data-uid="" data-ip="" data-loc="list"><b><b><b>운영자</b></b></b></td>
     <td class="gall_date" title="2015-12-17 17:01:41">15.12.17</td>
     <td class="gall_count">329904</td>
     <td class="gall_recommend">34</td>
    </tr>
    <tr class="ub-content us-post" data-no="4782971" data-type="icon_pic">
     <td class="gall_num">4782971</td>
     <td class="gall_tit ub-word"><a href="/board/view/?id=granblue&amp;no=4782971&amp;page=1" view-msg=""> <em class="icon_img icon_pic"></em>레페 추가될 수영복은 누굴까</a></td>
     <td class="gall_writer ub-writer" data-nick="KOSMOS" data-uid="301rs3xp0cfs" data-ip="" data-loc="list"><span class="nickname in" title="KOSMOS" style=""><em>KOSMOS</em></span><a class="writer_nikcon "><img src="https://nstatic.dcinside.com/dc/w/images/fix_nik.gif" border="0" title="301rs3xp0c** : 갤로그로 이동합니다." width="12" height="11" style="cursor:pointer;margin-left:2px;" onclick="window.open('//gallog.dcinside.com/301rs3xp0cfs');" alt="갤로그로 이동합니다."></a></td>
     <td class="gall_date" title="2024-07-19 15:59:28">15:59</td>
     <td class="gall_count">33</td>
     <td class="gall_recommend">0</td>
    </tr>
 */

