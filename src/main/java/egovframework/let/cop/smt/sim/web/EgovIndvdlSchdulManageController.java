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

    @org.springframework.beans.factory.annotation.Autowired
    private egovframework.com.cmm.util.MessageUtil messageUtil;


    @Resource(name = "propertiesService")
    private EgovPropertyService propertyService;

    /**
     * 일정 목록 조회
     */
    @GetMapping({"", "/"})
    public String scheduleList(@ModelAttribute ComDefaultVO searchVO,
                               @AuthenticationPrincipal LoginVO user,
                               Model model) throws Exception {
        if (searchVO.getPageIndex() < 1) searchVO.setPageIndex(1);

        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
        paginationInfo.setRecordCountPerPage(propertyService.getInt("Globals.pageUnit"));
        paginationInfo.setPageSize(propertyService.getInt("Globals.pageSize"));
        searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
        searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

        // 본인 일정만 노출(관리자는 전체). uniqId 를 미사용 필드(searchKeywordFrom)에 실어 매퍼 필터로 전달.
        if (user != null && !"ROLE_ADMIN".equals(user.getGroupNm())) {
            searchVO.setSearchKeywordFrom(user.getUniqId());
        }

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
                vo.setSchdulChargerId(user.getUniqId());
            }
            // datetime-local(yyyy-MM-ddTHH:mm) → 매퍼가 기대하는 yyyyMMddHHmm 형식으로 정규화
            vo.setSchdulBgnde(normalizeDateTime(vo.getSchdulBgnde()));
            vo.setSchdulEndde(normalizeDateTime(vo.getSchdulEndde()));
            // 개인일정 기본 분류값(목록/조회 필터와 정합)
            if (vo.getSchdulKindCode() == null || vo.getSchdulKindCode().isEmpty()) vo.setSchdulKindCode("2");
            if (vo.getSchdulSe() == null || vo.getSchdulSe().isEmpty()) vo.setSchdulSe("1"); // 개인일정 구분(CHAR(1))

            schdulService.insertIndvdlSchdulManage(vo);
            ra.addFlashAttribute("successMsg", messageUtil.get("msg.sch.registered"));
            ra.addFlashAttribute("savedModal", true);
            return "redirect:/schedule";
        } catch (Exception e) {
            log.error("일정 등록 오류", e);
            ra.addFlashAttribute("errorMsg", messageUtil.get("msg.sch.register.error"));
            return "redirect:/schedule/write";
        }
    }

    /**
     * datetime-local 입력값(yyyy-MM-ddTHH:mm[:ss])을 숫자만 남긴 yyyyMMddHHmm(12자리)로 정규화한다.
     * 매퍼 INSERT/UPDATE 의 SUBSTRING 파싱 규칙과 일치시킨다.
     */
    private String normalizeDateTime(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("\\D", "");
        return digits.length() >= 12 ? digits.substring(0, 12) : digits;
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
                ra.addFlashAttribute("errorMsg", messageUtil.get("msg.sch.noEditPerm"));
                return "redirect:/schedule/" + schdulId + "/detail";
            }
            vo.setSchdulId(schdulId);
            if (user != null) vo.setLastUpdusrId(user.getUniqId());
            vo.setSchdulBgnde(normalizeDateTime(vo.getSchdulBgnde()));
            vo.setSchdulEndde(normalizeDateTime(vo.getSchdulEndde()));
            if (vo.getSchdulKindCode() == null || vo.getSchdulKindCode().isEmpty()) vo.setSchdulKindCode("2");
            if (vo.getSchdulSe() == null || vo.getSchdulSe().isEmpty()) vo.setSchdulSe("1"); // 개인일정 구분(CHAR(1))
            schdulService.updateIndvdlSchdulManage(vo);
            ra.addFlashAttribute("successMsg", messageUtil.get("msg.sch.updated"));
            ra.addFlashAttribute("savedModal", true);
            return "redirect:/schedule/" + schdulId + "/detail";
        } catch (Exception e) {
            log.error("일정 수정 오류", e);
            ra.addFlashAttribute("errorMsg", messageUtil.get("msg.sch.update.error"));
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
                ra.addFlashAttribute("errorMsg", messageUtil.get("msg.sch.noDeletePerm"));
                return "redirect:/schedule/" + schdulId + "/detail";
            }
            IndvdlSchdulManageVO vo = new IndvdlSchdulManageVO();
            vo.setSchdulId(schdulId);
            if (user != null) vo.setLastUpdusrId(user.getUniqId());
            schdulService.deleteIndvdlSchdulManage(vo);
            ra.addFlashAttribute("successMsg", messageUtil.get("msg.sch.deleted"));
        } catch (Exception e) {
            log.error("일정 삭제 오류", e);
            ra.addFlashAttribute("errorMsg", messageUtil.get("msg.sch.delete.error"));
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
