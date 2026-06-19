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
                                      @ModelAttribute MberManageVO mberVO,
                                      RedirectAttributes ra) {
        try {
            mberVO.setMberId(mberId);
            mberService.updateMber(mberVO);
            ra.addFlashAttribute("successMsg", "회원 정보가 수정되었습니다.");
            return "redirect:/member/" + mberId + "/detail";
        } catch (Exception e) {
            log.error("회원 수정 오류", e);
            ra.addFlashAttribute("errorMsg", "회원 정보 수정 중 오류가 발생했습니다.");
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
            ra.addFlashAttribute("successMsg", "회원이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("회원 삭제 오류", e);
            ra.addFlashAttribute("errorMsg", "회원 삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/member/list";
    }
}
