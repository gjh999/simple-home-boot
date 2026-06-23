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
     * 회원 비밀번호 초기화 (관리자 전용)
     * 임시 비밀번호(기본값)로 재설정한다. 저장값은 이중 SHA-256 해시 형식을 유지한다.
     * (1차 = Base64(SHA256(id||pw)), 2차(저장) = Base64(SHA256(id||1차)) = encryptPasswordTwice)
     */
    @PostMapping("/{mberId}/reset-password")
    public String memberResetPassword(@PathVariable String mberId, RedirectAttributes ra) {
        try {
            MberManageVO mber = mberService.selectMber(mberId);
            if (mber == null || mber.getUniqId() == null || mber.getUniqId().isEmpty()) {
                ra.addFlashAttribute("errorMsg", messageUtil.get("msg.mber.pwReset.error"));
                return "redirect:/member/list";
            }
            // 임시 비밀번호 기본값 (관리자 안내용 — 로그인 후 변경 권장)
            String tempPw = DEFAULT_RESET_PASSWORD;
            String enpassword = egovframework.let.utl.sim.service.EgovFileScrty
                    .encryptPasswordTwice(tempPw, mber.getMberId());
            mber.setPassword(enpassword);
            mberService.updatePassword(mber);

            ra.addFlashAttribute("successMsg",
                    messageUtil.get("msg.mber.pwReset.done") + " (" + tempPw + ")");
            return "redirect:/member/" + mberId + "/detail";
        } catch (Exception e) {
            log.error("회원 비밀번호 초기화 오류", e);
            ra.addFlashAttribute("errorMsg", messageUtil.get("msg.mber.pwReset.error"));
            return "redirect:/member/" + mberId + "/detail";
        }
    }

    /** 비밀번호 초기화 기본 임시값 (공개 샘플 — 운영 시 정책에 맞게 조정) */
    private static final String DEFAULT_RESET_PASSWORD = "egov1234!";

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
