package egovframework.let.cop.smt.sim.web;

import egovframework.com.cmm.ComDefaultVO;
import egovframework.com.cmm.LoginVO;
import egovframework.let.cop.smt.sim.service.EgovIndvdlSchdulManageService;
import egovframework.let.cop.smt.sim.service.IndvdlSchdulManageVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Thymeleaf 기반 일정관리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/schedule")
public class EgovIndvdlSchdulManageController {

    @Resource(name = "egovIndvdlSchdulManageService")
    private EgovIndvdlSchdulManageService schdulService;

    @Resource(name = "propertiesService")
    private EgovPropertyService propertyService;

    /**
     * 일정 목록 조회
     */
    @GetMapping({"", "/"})
    public String scheduleList(@ModelAttribute ComDefaultVO searchVO, Model model) throws Exception {
        if (searchVO.getPageIndex() < 1) searchVO.setPageIndex(1);

        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
        paginationInfo.setRecordCountPerPage(propertyService.getInt("Globals.pageUnit"));
        paginationInfo.setPageSize(propertyService.getInt("Globals.pageSize"));
        searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
        searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

        List<?> scheduleList = schdulService.selectIndvdlSchdulManageList(searchVO);
        int totalCnt = schdulService.selectIndvdlSchdulManageListCnt(searchVO);
        paginationInfo.setTotalRecordCount(totalCnt);

        model.addAttribute("scheduleList", scheduleList);
        model.addAttribute("paginationInfo", paginationInfo);
        model.addAttribute("searchVO", searchVO);
        return "let/cop/smt/sim/scheduleList";
    }

    /**
     * 일정 상세 조회
     */
    @GetMapping("/{schdulId}/detail")
    public String scheduleDetail(@PathVariable String schdulId, Model model) throws Exception {
        IndvdlSchdulManageVO vo = new IndvdlSchdulManageVO();
        vo.setSchdulId(schdulId);
        IndvdlSchdulManageVO detail = schdulService.selectIndvdlSchdulManageDetail(vo);
        model.addAttribute("schedule", detail);
        return "let/cop/smt/sim/scheduleDetail";
    }

    /**
     * 일정 등록 폼
     */
    @GetMapping("/write")
    public String scheduleWriteForm(Model model) {
        model.addAttribute("schedule", new IndvdlSchdulManageVO());
        return "let/cop/smt/sim/scheduleWrite";
    }

    /**
     * 일정 등록 처리
     */
    @PostMapping("/write")
    public String scheduleWriteProcess(@ModelAttribute IndvdlSchdulManageVO vo,
                                       @AuthenticationPrincipal LoginVO user,
                                       RedirectAttributes ra) {
        try {
            if (user != null) {
                vo.setFrstRegisterId(user.getUniqId());
                vo.setLastUpdusrId(user.getUniqId());
                vo.setSchdulChargerId(user.getId());
            }
            schdulService.insertIndvdlSchdulManage(vo);
            ra.addFlashAttribute("successMsg", "일정이 등록되었습니다.");
            return "redirect:/schedule";
        } catch (Exception e) {
            log.error("일정 등록 오류", e);
            ra.addFlashAttribute("errorMsg", "일정 등록 중 오류가 발생했습니다.");
            return "redirect:/schedule/write";
        }
    }

    /**
     * 일정 수정 폼
     */
    @GetMapping("/{schdulId}/update")
    public String scheduleUpdateForm(@PathVariable String schdulId, Model model) throws Exception {
        IndvdlSchdulManageVO vo = new IndvdlSchdulManageVO();
        vo.setSchdulId(schdulId);
        IndvdlSchdulManageVO detail = schdulService.selectIndvdlSchdulManageDetail(vo);
        model.addAttribute("schedule", detail);
        return "let/cop/smt/sim/scheduleUpdate";
    }

    /**
     * 일정 수정 처리
     */
    @PostMapping("/{schdulId}/update")
    public String scheduleUpdateProcess(@PathVariable String schdulId,
                                        @ModelAttribute IndvdlSchdulManageVO vo,
                                        @AuthenticationPrincipal LoginVO user,
                                        RedirectAttributes ra) {
        try {
            // 소유자(작성자) 또는 관리자만 수정 가능
            if (!isOwnerOrAdmin(schdulId, user)) {
                ra.addFlashAttribute("errorMsg", "해당 일정을 수정할 권한이 없습니다.");
                return "redirect:/schedule/" + schdulId + "/detail";
            }
            vo.setSchdulId(schdulId);
            if (user != null) vo.setLastUpdusrId(user.getUniqId());
            schdulService.updateIndvdlSchdulManage(vo);
            ra.addFlashAttribute("successMsg", "일정이 수정되었습니다.");
            return "redirect:/schedule/" + schdulId + "/detail";
        } catch (Exception e) {
            log.error("일정 수정 오류", e);
            ra.addFlashAttribute("errorMsg", "일정 수정 중 오류가 발생했습니다.");
            return "redirect:/schedule/" + schdulId + "/update";
        }
    }

    /**
     * 일정 삭제 처리
     */
    @PostMapping("/{schdulId}/delete")
    public String scheduleDeleteProcess(@PathVariable String schdulId,
                                        @AuthenticationPrincipal LoginVO user,
                                        RedirectAttributes ra) {
        try {
            // 소유자(작성자) 또는 관리자만 삭제 가능
            if (!isOwnerOrAdmin(schdulId, user)) {
                ra.addFlashAttribute("errorMsg", "해당 일정을 삭제할 권한이 없습니다.");
                return "redirect:/schedule/" + schdulId + "/detail";
            }
            IndvdlSchdulManageVO vo = new IndvdlSchdulManageVO();
            vo.setSchdulId(schdulId);
            if (user != null) vo.setLastUpdusrId(user.getUniqId());
            schdulService.deleteIndvdlSchdulManage(vo);
            ra.addFlashAttribute("successMsg", "일정이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("일정 삭제 오류", e);
            ra.addFlashAttribute("errorMsg", "일정 삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/schedule";
    }

    /**
     * 일정의 작성자 본인 또는 관리자(ROLE_ADMIN)인지 검증한다.
     * 존재하지 않는 일정이거나 권한이 없으면 false.
     */
    private boolean isOwnerOrAdmin(String schdulId, LoginVO user) {
        if (user == null || user.getUniqId() == null) {
            return false;
        }
        if ("ROLE_ADMIN".equals(user.getGroupNm())) {
            return true;
        }
        try {
            IndvdlSchdulManageVO probe = new IndvdlSchdulManageVO();
            probe.setSchdulId(schdulId);
            IndvdlSchdulManageVO detail = schdulService.selectIndvdlSchdulManageDetail(probe);
            return detail != null && user.getUniqId().equals(detail.getFrstRegisterId());
        } catch (Exception e) {
            log.error("일정 권한 검증 오류", e);
            return false;
        }
    }
}
