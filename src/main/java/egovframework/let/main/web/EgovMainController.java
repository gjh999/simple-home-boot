package egovframework.let.main.web;

import egovframework.let.cop.bbs.dto.request.BbsSearchRequestDTO;
import egovframework.let.cop.bbs.dto.response.BbsManageListResponseDTO;
import egovframework.let.cop.bbs.service.EgovBBSManageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Thymeleaf 기반 메인 페이지 컨트롤러
 */
@Slf4j
@Controller
public class EgovMainController {

    @Resource(name = "EgovBBSManageService")
    private EgovBBSManageService bbsMngService;

    /**
     * 포털 대시보드 (공지/갤러리 요약 + 바로가기)
     */
    @GetMapping("/portal")
    public String mainPage(Model model) {
        try {
            PaginationInfo paginationInfo = new PaginationInfo();
            paginationInfo.setCurrentPageNo(1);
            paginationInfo.setRecordCountPerPage(5);
            paginationInfo.setPageSize(10);

            // 공지사항 목록 (상위 5건)
            BbsSearchRequestDTO notiSearch = new BbsSearchRequestDTO();
            notiSearch.setBbsId("BBSMSTR_AAAAAAAAAAAA");
            BbsManageListResponseDTO notiList = bbsMngService.selectBoardArticles(notiSearch, paginationInfo, "BBSA02");
            model.addAttribute("notiList", notiList.getResultList());

            // 갤러리 목록 (상위 5건)
            BbsSearchRequestDTO galSearch = new BbsSearchRequestDTO();
            galSearch.setBbsId("BBSMSTR_BBBBBBBBBBBB");
            BbsManageListResponseDTO galList = bbsMngService.selectBoardArticles(galSearch, paginationInfo, "BBSA02");
            model.addAttribute("galList", galList.getResultList());

        } catch (Exception e) {
            log.warn("메인 페이지 조회 중 오류 (DB 미설정 시 정상): {}", e.getMessage());
        }

        return "let/main/mainView";
    }
}
