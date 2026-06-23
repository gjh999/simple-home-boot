package egovframework.let.uss.umt.web;

import egovframework.let.uss.umt.service.EgovMberManageService;
import egovframework.let.uss.umt.service.MberManageVO;
import egovframework.let.uss.umt.service.UserDefaultVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Thymeleaf 기반 회원관리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/member")
public class EgovMberManageController {

    @Resource(name = "mberManageService")
    private EgovMberManageService mberService;

    @org.springframework.beans.factory.annotation.Autowired
    private egovframework.com.cmm.util.MessageUtil messageUtil;


    @Resource(name = "propertiesService")
    private EgovPropertyService propertyService;

    /**
     * 회원 목록 조회
     */
    @GetMapping("/list")
    public String memberList(@ModelAttribute UserDefaultVO searchVO, Model model) throws Exception {
        if (searchVO.getPageIndex() < 1) searchVO.setPageIndex(1);

        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
        paginationInfo.setRecordCountPerPage(propertyService.getInt("Globals.pageUnit"));
        paginationInfo.setPageSize(propertyService.getInt("Globals.pageSize"));
        searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
        searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

        List<MberManageVO> memberList = mberService.selectMberList(searchVO);
        int totalCnt = mberService.selectMberListTotCnt(searchVO);
        paginationInfo.setTotalRecordCount(totalCnt);

        model.addAttribute("memberList", memberList);
        model.addAttribute("paginationInfo", paginationInfo);
        model.addAttribute("searchVO", searchVO);
        return "let/uss/umt/memberList";
    }

    /**
     * 회원 상세 조회
     */
    @GetMapping("/{mberId}/detail")
    public String memberDetail(@PathVariable String mberId, Model model) throws Exception {
        MberManageVO member = mberService.selectMber(mberId);
        model.addAttribute("member", member);
        return "let/uss/umt/memberDetail";
    }

    /**
     * 회원 정보 수정 폼
     */
    @GetMapping("/{mberId}/update")
    public String memberUpdateForm(@PathVariable String mberId, Model model) throws Exception {
        MberManageVO member = mberService.selectMber(mberId);
        model.addAttribute("member", member);
        return "let/uss/umt/memberUpdate";
    }

    /**
     * 회원 정보 수정 처리
     */
    @PostMapping("/{mberId}/update")
    public String memberUpdateProcess(@PathVariable String mberId,
                                      @ModelAttribute MberManageVO formVO,
                                      RedirectAttributes ra) {
        try {
            // 기존 정보를 불러와 편집 항목만 변경 (비밀번호/상태/고유ID 등 미입력 항목 보존)
            MberManageVO mber = mberService.selectMber(mberId);
            if (mber == null) {
                ra.addFlashAttribute("errorMsg", messageUtil.get("msg.mber.update.error"));
                return "redirect:/member/list";
            }
            mber.setMberNm(formVO.getMberNm());
            mber.setMberEmailAdres(formVO.getMberEmailAdres());
            mber.setMoblphonNo(formVO.getMoblphonNo());
            mber.setZip(formVO.getZip());
            mber.setAdres(formVO.getAdres());
            mber.setPassword(""); // 빈 값 → 비밀번호는 변경하지 않음(updateMber 가 해시/SET 생략)

            mberService.updateMber(mber);
            ra.addFlashAttribute("successMsg", messageUtil.get("msg.mber.updated"));
            return "redirect:/member/" + mberId + "/detail";
        } catch (Exception e) {
            log.error("회원 수정 오류", e);
            ra.addFlashAttribute("errorMsg", messageUtil.get("msg.mber.update.error"));
            return "redirect:/member/" + mberId + "/update";
        }
    }

    /**
     * 회원 삭제 처리
     */
    @PostMapping("/{mberId}/delete")
    public String memberDeleteProcess(@PathVariable String mberId, RedirectAttributes ra) {
        try {
            mberService.deleteMber(mberId);
            ra.addFlashAttribute("successMsg", messageUtil.get("msg.mber.deleted"));
        } catch (Exception e) {
            log.error("회원 삭제 오류", e);
            ra.addFlashAttribute("errorMsg", messageUtil.get("msg.mber.delete.error"));
        }
        return "redirect:/member/list";
    }
}
