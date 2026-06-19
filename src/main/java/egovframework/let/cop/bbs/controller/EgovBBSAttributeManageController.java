package egovframework.let.cop.bbs.controller;

import java.util.List;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import egovframework.com.cmm.ComDefaultCodeVO;
import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.service.CmmnDetailCode;
import egovframework.com.cmm.service.EgovCmmUseService;
import egovframework.let.cop.bbs.dto.request.BbsAttributeInsertRequestDTO;
import egovframework.let.cop.bbs.dto.request.BbsAttributeUpdateRequestDTO;
import egovframework.let.cop.bbs.dto.request.BbsSearchRequestDTO;
import egovframework.let.cop.bbs.dto.response.BbsAttributeListResponseDTO;
import egovframework.let.cop.bbs.dto.response.BbsFileAtchResponseDTO;
import egovframework.let.cop.bbs.enums.BbsDetailRequestType;
import egovframework.let.cop.bbs.service.EgovBBSAttributeManageService;

/**
 * Thymeleaf 기반 게시판 마스터 관리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/bbs/master")
public class EgovBBSAttributeManageController {

    @Resource(name = "EgovBBSAttributeManageService")
    private EgovBBSAttributeManageService bbsAttrbService;

    @Resource(name = "EgovCmmUseService")
    private EgovCmmUseService cmmUseService;

    @Resource(name = "propertiesService")
    private EgovPropertyService propertyService;

    /**
     * 게시판 마스터 목록 조회
     */
    @GetMapping("/list")
    public String masterList(@ModelAttribute BbsSearchRequestDTO search, Model model) throws Exception {
        if (search.getPageIndex() < 1) search.setPageIndex(1);

        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(search.getPageIndex());
        paginationInfo.setRecordCountPerPage(propertyService.getInt("Globals.pageUnit"));
        paginationInfo.setPageSize(propertyService.getInt("Globals.pageSize"));

        BbsAttributeListResponseDTO result = bbsAttrbService.selectBBSMasterInfs(search, paginationInfo);
        paginationInfo.setTotalRecordCount(result.getResultCnt());
        result.setPaginationInfo(paginationInfo);

        model.addAttribute("result", result);
        model.addAttribute("paginationInfo", paginationInfo);
        model.addAttribute("search", search);
        return "let/cop/bbs/master/bbsMasterList";
    }

    /**
     * 게시판 마스터 상세 조회
     */
    @GetMapping("/{bbsId}/detail")
    public String masterDetail(@PathVariable String bbsId, Model model) throws Exception {
        BbsFileAtchResponseDTO detail = bbsAttrbService.selectBBSMasterInf(bbsId, null, BbsDetailRequestType.DETAIL);
        model.addAttribute("detail", detail);
        model.addAttribute("bbsId", bbsId);
        return "let/cop/bbs/master/bbsMasterDetail";
    }

    /**
     * 게시판 마스터 등록 폼
     */
    @GetMapping("/write")
    public String masterWriteForm(Model model) throws Exception {
        ComDefaultCodeVO vo = new ComDefaultCodeVO();
        vo.setCodeId("COM004");
        List<CmmnDetailCode> typeList = cmmUseService.selectCmmCodeDetail(vo);
        vo.setCodeId("COM009");
        List<CmmnDetailCode> attrbList = cmmUseService.selectCmmCodeDetail(vo);

        model.addAttribute("typeList", typeList);
        model.addAttribute("attrbList", attrbList);
        return "let/cop/bbs/master/bbsMasterWrite";
    }

    /**
     * 게시판 마스터 등록 처리
     */
    @PostMapping("/write")
    public String masterWriteProcess(@ModelAttribute BbsAttributeInsertRequestDTO dto,
                                     @AuthenticationPrincipal LoginVO loginVO,
                                     RedirectAttributes ra) {
        try {
            dto.setFrstRegisterId(loginVO.getUniqId());
            dto.setUseAt("Y");
            dto.setTrgetId("SYSTEM_DEFAULT_BOARD");
            dto.setPosblAtchFileSize(propertyService.getString("Globals.posblAtchFileSize"));
            bbsAttrbService.insertBBSMastetInf(dto);
            ra.addFlashAttribute("successMsg", "게시판이 등록되었습니다.");
        } catch (Exception e) {
            log.error("게시판 등록 오류", e);
            ra.addFlashAttribute("errorMsg", "게시판 등록 중 오류가 발생했습니다.");
        }
        return "redirect:/bbs/master/list";
    }

    /**
     * 게시판 마스터 수정 폼
     */
    @GetMapping("/{bbsId}/update")
    public String masterUpdateForm(@PathVariable String bbsId, Model model) throws Exception {
        BbsFileAtchResponseDTO detail = bbsAttrbService.selectBBSMasterInf(bbsId, null, BbsDetailRequestType.DETAIL);

        ComDefaultCodeVO vo = new ComDefaultCodeVO();
        vo.setCodeId("COM004");
        List<CmmnDetailCode> typeList = cmmUseService.selectCmmCodeDetail(vo);
        vo.setCodeId("COM009");
        List<CmmnDetailCode> attrbList = cmmUseService.selectCmmCodeDetail(vo);

        model.addAttribute("detail", detail);
        model.addAttribute("bbsId", bbsId);
        model.addAttribute("typeList", typeList);
        model.addAttribute("attrbList", attrbList);
        return "let/cop/bbs/master/bbsMasterUpdate";
    }

    /**
     * 게시판 마스터 수정 처리
     */
    @PostMapping("/{bbsId}/update")
    public String masterUpdateProcess(@PathVariable String bbsId,
                                      @ModelAttribute BbsAttributeUpdateRequestDTO dto,
                                      @AuthenticationPrincipal LoginVO loginVO,
                                      RedirectAttributes ra) {
        try {
            dto.setBbsId(bbsId);
            dto.setLastUpdusrId(loginVO.getUniqId());
            dto.setPosblAtchFileSize(propertyService.getString("Globals.posblAtchFileSize"));
            bbsAttrbService.updateBBSMasterInf(dto);
            ra.addFlashAttribute("successMsg", "게시판 정보가 수정되었습니다.");
        } catch (Exception e) {
            log.error("게시판 수정 오류", e);
            ra.addFlashAttribute("errorMsg", "게시판 수정 중 오류가 발생했습니다.");
        }
        return "redirect:/bbs/master/" + bbsId + "/detail";
    }

    /**
     * 게시판 마스터 삭제 처리
     */
    @PostMapping("/{bbsId}/delete")
    public String masterDeleteProcess(@PathVariable String bbsId,
                                      @AuthenticationPrincipal LoginVO loginVO,
                                      RedirectAttributes ra) {
        try {
            bbsAttrbService.deleteBBSMasterInf(loginVO.getUniqId(), bbsId);
            ra.addFlashAttribute("successMsg", "게시판이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("게시판 삭제 오류", e);
            ra.addFlashAttribute("errorMsg", "게시판 삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/bbs/master/list";
    }
}
