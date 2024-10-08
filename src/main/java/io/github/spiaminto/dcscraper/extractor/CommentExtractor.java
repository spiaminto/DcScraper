package io.github.spiaminto.dcscraper.extractor;

import io.github.spiaminto.dcscraper.dto.DcComment;
import io.github.spiaminto.dcscraper.properties.CommentExtractorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentExtractor {

    private final CommentExtractorProperties props;

    /**
     * 댓글 li 요소 리스트 (.cmt_list>li) 에서 댓글 또는 답글을 추출
     *
     * @param boardNum 게시글 번호
     * @param liElement  댓글 li 요소 (댓글 1 개 or 답글리스트)
     * @return 댓글과 답글 리스트 (댓글일 경우 size = 1, 답글리스트 일경우 size = 답글갯수, 댓글돌이 등 추출불가 size = 0)
     */
    public List<DcComment> extractCommentAndReply(long boardNum, Element liElement, Element liElementPrev) {
        List<DcComment> results = new ArrayList<>(); // 결과

        if (isDory(liElement)) {
            return new ArrayList<>();
        } // 댓글돌이 제외

        if (!isReply(liElement)) { // 답글 리스트 여부 확인 (댓글 or 답글list 로 분기)
            // 댓글
            if (isDeleted(liElement)) { // 삭제여부 확인
                // 삭제된 댓글
                results.add(extractDeletedComment(boardNum, liElement));
            } else {
                // 일반 댓글
                results.add(extractComment(boardNum, liElement));
            }
        } else {
            // 답글 리스트
            Elements liElementsReply = liElement.select(props.getReplyListItemSelector());
            // 답글 리스트 루프
            for (Element liElementReply : liElementsReply) {
                if (isDeleted(liElementReply)) { // 삭제여부 확인
                    // 삭제된 답글
                    results.add(extractDeletedReply(boardNum, liElementReply));
                } else {
                    // 답글
                    long targetId;
                    if (liElementPrev == null || liElementPrev.select(props.getCommentNumSelector()).isEmpty()) {
                        // 이전 댓글이 삭제되었거나, 댓글이 아닌 경우 targetNum 을 -1 로 설정
                        targetId = -1;
                    } else {
                        targetId = Long.parseLong(selectBy(liElementPrev, props.getCommentNumSelector(), props.getCommentNumAttr()));
                    }
                    results.add(extractReply(boardNum, liElementReply, targetId));
                }

            }
        }
//            log.info("CommentExtractor result: {}", result);
        return results;
    }

    protected boolean isReply(Element liElement) {
        return presenceSelectBy(liElement, props.getIsReplySelector());
    }

    protected boolean isDeleted(Element liElement) {
        return presenceSelectBy(liElement, props.getIsDeletedSelector());
    }

    protected boolean isDory(Element liElement) {
        return presenceSelectBy(liElement, props.getIsDorySelector());
    }

    protected DcComment extractComment(long boardNum, Element listItem) {
        String commentNum = selectBy(listItem, props.getCommentNumSelector(), props.getCommentNumAttr());
        String writer = selectBy(listItem, props.getCommentWriterSelector(), props.getCommentWriterAttr());
        String content = selectBy(listItem, props.getCommentContentSelector(), props.getCommentContentAttr());
        String regDate = selectBy(listItem, props.getCommentRegDateSelector(), props.getCommentRegDateAttr());

        LocalDateTime parsedTime = parseTime(regDate);

        return DcComment.builder()
                .boardNum(boardNum)
                .commentNum(Long.parseLong(commentNum))
                .writer(writer)
                .content(content)
                .regDate(parsedTime)
                .reply(false)
                .build();
    }

    public DcComment extractReply(long boardNum, Element listItem, long targetNum) {
        String replyNum = selectBy(listItem, props.getReplyNumSelector(), props.getReplyNumAttr());
        String writer = selectBy(listItem, props.getCommentWriterSelector(), props.getCommentWriterAttr());
        String content = selectBy(listItem, props.getCommentContentSelector(), props.getCommentContentAttr());
        String regDate = selectBy(listItem, props.getCommentRegDateSelector(), props.getCommentRegDateAttr());
        LocalDateTime parsedTime = parseTime(regDate);

        return DcComment.builder()
                .boardNum(boardNum)
                .commentNum(Long.parseLong(replyNum))
                .writer(writer)
                .content(content)
                .regDate(parsedTime)
                .reply(true)
                .targetNum(targetNum)
                .build();
    }

    public DcComment extractDeletedComment(long boardNum, Element liElement) {
        String commentNum = selectBy(liElement, props.getCommentNumSelector(), props.getCommentNumAttr());
        String content = selectBy(liElement, props.getIsDeletedSelector(), props.getIsDeletedAttr());

        return DcComment.builder()
                .boardNum(boardNum)
                .commentNum(Long.parseLong(commentNum))
                .content(content)
                .reply(false)
                .build();
    }

    public DcComment extractDeletedReply(long boardNum, Element liElement) {
        String commentNum = selectBy(liElement, props.getReplyNumSelector(), props.getReplyNumAttr());
        String content = selectBy(liElement, props.getIsDeletedSelector(), props.getIsDeletedAttr());

        return DcComment.builder()
                .boardNum(boardNum)
                .commentNum(Long.parseLong(commentNum))
                .content(content)
                .reply(false)
                .build();
    }

    protected LocalDateTime parseTime(String regDate) {
        // 날짜 변환 형식 2024.07.19 12:05:43 or 07-19 12:05:43
        regDate = regDate.length() > 14 ? regDate : LocalDateTime.now().getYear() + "." + regDate;
        DateTimeFormatter replyFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
        return LocalDateTime.parse(regDate, replyFormatter);
    }

    /**
     * 주어진 선택자와 속성을 이용하여 요소에서 필요한 내용 추출
     *
     * @param element
     * @param selector
     * @param attr
     * @return 주어진 선택자로 찾은 첫번째 요소의 속성 값
     */
    protected String selectBy(Element element, String selector, String attr) {
        try {
            if (attr.equals("text")) {
                return element.select(selector).first().text();
            } else if (attr.equals("innerHtml")) {
                return element.select(selector).first().html();
            } else if (attr.equals("outerHtml")) {
                return element.select(selector).first().outerHtml();
            } else {
                return element.select(selector).first().attr(attr);
            }
        } catch (Exception e) { // 에러난 요소 확인용 catch
            log.error("[COMMENT EXTRACTOR] selectBy " +
                    "element = \n" +
                    "{}\n" +
                    " selector = \n" +
                    "  {}\n " +
                    " attr = \n" +
                    "  {}\n" +
                    " error: \n{}\n" +
                    " message: \n{}", element, selector, attr, e, e.getMessage());
            throw e; // 에러 처리 X
        }
    }

    /**
     * 주어진 선택자를 이용하여 요소의 존재 유무를 판단
     *
     * @param element
     * @param selector
     * @return 존재하면 true
     */
    protected boolean presenceSelectBy(Element element, String selector) {
        return !element.select(selector).isEmpty();
    }

}


/*
 댓글 답글 스크래핑 예시

<li id="comment_li_14233925" class="ub-content">
 <div class="cmt_info clear" data-no="14233925" data-rcnt="3" data-article-no="4798106">
  <div class="cmt_nickbox">
   <span class="gall_writer ub-writer" data-nick="신한C" data-uid="jpjp1129" data-ip=""><span class="nickname in" title="신한C" style=""><em>신한C</em></span> <a class="writer_nikcon "><img src="https://nstatic.dcinside.com/dc/w/images/fix_nik.gif" border="0" title="jpjp11** : 갤로그로 이동합니다." width="12" height="11" style="cursor:pointer;" onclick="window.open('//gallog.dcinside.com/jpjp1129');" alt="갤로그로 이동합니다."></a></span>
  </div>
  <div class="clear cmt_txtbox btn_reply_write_all">
   <p class="usertxt ub-word">뒤져</p>
  </div>
  <div class="fr clear">
   <span class="date_time">08.06 16:23:28</span>
   <div class="cmt_mdf_del " data-type="cmt" re_no="14233925" data-my="N" data-article-no="4798106" data-pwd-pop="Y" data-uid="jpjp1129"></div>
  </div>
 </div>
</li>
답글(reply)는 comment_li_... 과 동일 depth 에서 시작, 내부에 reply_list 를 가짐
<li>
 <div class="reply show">
  <div class="reply_box">
   <ul class="reply_list" id="reply_list_14233925">
    <li id="reply_li_14233928" class="ub-content">
     <div class="reply_info clear" data-no="14233928">
      <div class="cmt_nickbox">
       <span class="gall_writer ub-writer" data-nick="정보소양" data-uid="shept123" data-ip=""><span class="nickname me in" title="정보소양" style=""><em>정보소양</em></span> <a class="writer_nikcon "><img src="https://nstatic.dcinside.com/dc/w/images/fix_nik.gif" border="0" title="shept1** : 갤로그로 이동합니다." width="12" height="11" style="cursor:pointer;" onclick="window.open('//gallog.dcinside.com/shept123');" alt="갤로그로 이동합니다."></a></span>
      </div>
      <div class="clear cmt_txtbox">
       <p class="usertxt ub-word">나쁜말한번만더해봐라진짜</p>
      </div>
      <div class="fr clear">
       <span class="date_time">08.06 16:24:06</span>
      </div>
     </div></li>
    <li id="reply_li_14233930" class="ub-content">
     <div class="reply_info clear" data-no="14233930">
      <div class="cmt_nickbox">
       <span class="gall_writer ub-writer" data-nick="신한C" data-uid="jpjp1129" data-ip=""><span class="nickname in" title="신한C" style=""><em>신한C</em></span> <a class="writer_nikcon "><img src="https://nstatic.dcinside.com/dc/w/images/fix_nik.gif" border="0" title="jpjp11** : 갤로그로 이동합니다." width="12" height="11" style="cursor:pointer;" onclick="window.open('//gallog.dcinside.com/jpjp1129');" alt="갤로그로 이동합니다."></a></span>
      </div>
      <div class="clear cmt_txtbox">
       <p class="usertxt ub-word">뒤져(하트)</p>
      </div>
      <div class="fr clear">
       <span class="date_time">08.06 16:24:39</span>
      </div>
     </div></li>
    <li id="reply_li_14233931" class="ub-content">
     <div class="reply_info clear" data-no="14233931">
      <div class="cmt_nickbox">
       <span class="gall_writer ub-writer" data-nick="정보소양" data-uid="shept123" data-ip=""><span class="nickname me in" title="정보소양" style=""><em>정보소양</em></span> <a class="writer_nikcon "><img src="https://nstatic.dcinside.com/dc/w/images/fix_nik.gif" border="0" title="shept1** : 갤로그로 이동합니다." width="12" height="11" style="cursor:pointer;" onclick="window.open('//gallog.dcinside.com/shept123');" alt="갤로그로 이동합니다."></a></span>
      </div>
      <div class="clear cmt_txtbox">
       <div class="comment_dccon clear">
        <div class="coment_dccon_img ">
         <img class="written_dccon " src="https://dcimg5.dcinside.com/dccon.php?no=62b5df2be09d3ca567b1c5bc12d46b394aa3b1058c6e4d0ca41648b658ea2574147f57745397cd0897f50d707d51990e7d54f43e05dcf62cf58cabda7d77ef3bd062053d419be66ab40d1b448b" conalt="21" alt="21" title="21" data-dcconoverstatus="false">
        </div>
        <div class="coment_dccon_info clear dccon_over_box" onmouseover="dccon_btn_over(this);" onmouseout="dccon_btn_over(this);" style="display:none;">
         <span class="over_alt"></span><button type="button" class="btn_dccon_infoview div_package" data-type="reply" onclick="dccon_btn_click();" reqpath="/dccon">디시콘 보기</button>
        </div>
       </div>
      </div>
      <div class="fr clear">
       <span class="date_time">08.06 16:24:52</span>
      </div>
     </div></li>
   </ul>
  </div>
 </div>
</li>

 */
