package egovframework.let.cop.com.web;

import java.util.Map;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import egovframework.com.cmm.LoginVO;
import egovframework.let.cop.bbs.domain.model.BoardMasterVO;
import egovframework.let.cop.bbs.dto.request.BbsSearchRequestDTO;
import egovframework.let.cop.bbs.service.EgovBBSAttributeManageService;
import egovframework.let.cop.com.service.BoardUseInfVO;
import egovframework.let.cop.com.service.EgovBBSUseInfoManageService;

/**
 * Thymeleaf 기반 게시판 사용정보 관리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/bbs/use")
public class EgovBBSUseInfoManageController {

    @Resource(name = "EgovBBSUseInfoManageService")
    private EgovBBSUseInfoManageService bbsUseService;

    @Resource(name = "EgovBBSAttributeManageService")
    private EgovBBSAttributeManageService bbsAttrbService;

    @Resource(name = "propertiesService")
    private EgovPropertyService propertyService;

    /**
     * 게시판 사용정보 목록 조회
     */
    @GetMapping("/list")
    public String useList(@ModelAttribute BbsSearchRequestDTO search, Model model) throws Exception {
        BoardUseInfVO bdUseVO = new BoardUseInfVO();
        if (search.getPageIndex() < 1) search.setPageIndex(1);

        bdUseVO.setPageIndex(search.getPageIndex());
        bdUseVO.setSearchWrd(search.getSearchWrd() != null ? search.getSearchWrd() : "");
        bdUseVO.setPageUnit(propertyService.getInt("Globals.pageUnit"));
        bdUseVO.setPageSize(propertyService.getInt("Globals.pageSize"));

        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(bdUseVO.getPageIndex());
        paginationInfo.setRecordCountPerPage(bdUseVO.getPageUnit());
        paginationInfo.setPageSize(bdUseVO.getPageSize());

        bdUseVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        bdUseVO.setLastIndex(paginationInfo.getLastRecordIndex());
        bdUseVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

        Map<String, Object> map = bbsUseService.selectBBSUseInfs(bdUseVO);
        int totCnt = Integer.parseInt((String) map.get("resultCnt"));
        paginationInfo.setTotalRecordCount(totCnt);

        model.addAttribute("resultList", map.get("resultList"));
        model.addAttribute("resultCnt", totCnt);
        model.addAttribute("paginationInfo", paginationInfo);
        model.addAttribute("search", search);
        return "let/cop/bbs/use/bbsUseList";
    }

    /**
     * 게시판 사용정보 상세 조회
     */
    @GetMapping("/{trgetId}/{bbsId}/detail")
    public String useDetail(@PathVariable String trgetId,
                            @PathVariable String bbsId,
                            Model model) throws Exception {
        BoardUseInfVO bdUseVO = new BoardUseInfVO();
        bdUseVO.setBbsId(bbsId);
        bdUseVO.setTrgetId(trgetId);
        BoardUseInfVO vo = bbsUseService.selectBBSUseInf(bdUseVO);

        if ("SYSTEM_DEFAULT_BOARD".equals(vo.getTrgetId()) && !"BBST02".equals(vo.getBbsTyCode())) {
            vo.setProvdUrl("/bbs/" + bbsId + "/list");
        }

        BoardMasterVO boardMasterVO = new BoardMasterVO();
        Map<String, Object> notUsedMap = bbsAttrbService.selectNotUsedBdMstrList(boardMasterVO);

        model.addAttribute("bdUseVO", vo);
        model.addAttribute("notUsedList", notUsedMap.get("resultList"));
        model.addAttribute("trgetId", trgetId);
        model.addAttribute("bbsId", bbsId);
        return "let/cop/bbs/use/bbsUseDetail";
    }

    /**
     * 게시판 사용정보 등록 폼
     */
    @GetMapping("/write")
    public String useWriteForm(Model model) throws Exception {
        BoardMasterVO boardMasterVO = new BoardMasterVO();
        Map<String, Object> notUsedMap = bbsAttrbService.selectNotUsedBdMstrList(boardMasterVO);
        model.addAttribute("notUsedList", notUsedMap.get("resultList"));
        return "let/cop/bbs/use/bbsUseWrite";
    }

    /**
     * 게시판 사용정보 등록 처리
     */
    @PostMapping("/write")
    public String useWriteProcess(@ModelAttribute BoardUseInfVO bdUseVO,
                                  @AuthenticationPrincipal LoginVO loginVO,
                                  RedirectAttributes ra) {
        try {
            if ("CMMNTY".equals(bdUseVO.getTrgetType())) {
                bdUseVO.setRegistSeCode("REGC06");
            } else if ("CLUB".equals(bdUseVO.getTrgetType())) {
                bdUseVO.setRegistSeCode("REGC05");
            } else {
                bdUseVO.setRegistSeCode("REGC01");
            }
            bdUseVO.setUseAt("Y");
            bdUseVO.setFrstRegisterId(loginVO.getUniqId());
            bbsUseService.insertBBSUseInf(bdUseVO);
            ra.addFlashAttribute("successMsg", "게시판 사용정보가 등록되었습니다.");
        } catch (Exception e) {
            log.error("게시판 사용정보 등록 오류", e);
            ra.addFlashAttribute("errorMsg", "등록 중 오류가 발생했습니다.");
        }
        return "redirect:/bbs/use/list";
    }

    /**
     * 게시판 사용정보 수정 처리
     */
    @PostMapping("/{trgetId}/{bbsId}/update")
    public String useUpdateProcess(@PathVariable String trgetId,
                                   @PathVariable String bbsId,
                                   @ModelAttribute BoardUseInfVO bdUseVO,
                                   @AuthenticationPrincipal LoginVO loginVO,
                                   RedirectAttributes ra) {
        try {
            bdUseVO.setBbsId(bbsId);
            bdUseVO.setTrgetId(trgetId);
            bbsUseService.updateBBSUseInf(bdUseVO);
            ra.addFlashAttribute("successMsg", "게시판 사용정보가 수정되었습니다.");
        } catch (Exception e) {
            log.error("게시판 사용정보 수정 오류", e);
            ra.addFlashAttribute("errorMsg", "수정 중 오류가 발생했습니다.");
        }
        return "redirect:/bbs/use/" + trgetId + "/" + bbsId + "/detail";
    }
}
