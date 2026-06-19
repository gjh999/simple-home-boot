package egovframework.let.uss.umt.web;

import java.time.Duration;

import egovframework.com.cmm.LoginVO;
import egovframework.com.jwt.EgovJwtTokenUtil;
import egovframework.let.uss.umt.service.EgovMberManageService;
import egovframework.let.uss.umt.service.MberManageVO;
import egovframework.let.utl.sim.service.EgovFileScrty;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf 기반 마이페이지 컨트롤러
 *
 * <p>로그인한 사용자가 본인의 회원정보를 조회·수정하고 비밀번호를 변경한다.
 * 대상 데이터는 TB_EMPLYR_INFO 이며, 본인 식별은 JWT 의 uniqId(ESNTL_ID)로 한다.</p>
 */
@Slf4j
@Controller
public class EgovMyPageController {

    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

    @Resource(name = "mberManageService")
    private EgovMberManageService mberManageService;

    @Autowired
    private EgovJwtTokenUtil jwtTokenUtil;

    @Value("${Globals.jwt.cookieSecure:false}")
    private boolean cookieSecure;

    /**
     * 마이페이지 화면
     */
    @GetMapping(value = "/mypage", produces = MediaType.TEXT_HTML_VALUE)
    public String myPage(@AuthenticationPrincipal LoginVO user, Model model) throws Exception {
        if (user == null || user.getUniqId() == null) {
            return "redirect:/login";
        }
        MberManageVO mber = mberManageService.selectMber(user.getUniqId());
        model.addAttribute("mber", mber);
        return "let/uss/umt/myPage";
    }

    /**
     * 회원정보 수정 처리 (비밀번호 제외)
     */
    @PostMapping("/mypage/info")
    public String updateInfo(@AuthenticationPrincipal LoginVO user,
                             @RequestParam("mberNm") String mberNm,
                             @RequestParam(value = "mberEmailAdres", required = false) String mberEmailAdres,
                             @RequestParam(value = "moblphonNo", required = false) String moblphonNo,
                             @RequestParam(value = "zip", required = false) String zip,
                             @RequestParam(value = "adres", required = false) String adres,
                             @RequestParam(value = "detailAdres", required = false) String detailAdres,
                             HttpServletResponse response,
                             RedirectAttributes ra) {
        try {
            if (user == null || user.getUniqId() == null) {
                return "redirect:/login";
            }
            // 기존 정보를 불러와 편집 항목만 변경 (상태/권한/아이디 등은 보존)
            MberManageVO mber = mberManageService.selectMber(user.getUniqId());
            if (mber == null) {
                return "redirect:/login";
            }
            mber.setMberNm(mberNm);
            mber.setMberEmailAdres(nz(mberEmailAdres));
            mber.setMoblphonNo(nz(moblphonNo));
            mber.setZip(nz(zip));
            mber.setAdres(nz(adres));
            mber.setDetailAdres(nz(detailAdres));
            mber.setPassword(""); // 빈 값 → 비밀번호는 변경하지 않음

            mberManageService.updateMber(mber);

            // 헤더에 즉시 반영되도록 변경된 이름으로 JWT 재발급
            user.setName(mberNm);
            reissueToken(user, response);

            ra.addFlashAttribute("successMsg", "회원정보가 수정되었습니다.");
        } catch (Exception e) {
            log.error("회원정보 수정 오류", e);
            ra.addFlashAttribute("errorMsg", "회원정보 수정 중 오류가 발생했습니다.");
        }
        return "redirect:/mypage";
    }

    /**
     * 비밀번호 변경 처리
     */
    @PostMapping("/mypage/password")
    public String changePassword(@AuthenticationPrincipal LoginVO user,
                                 @RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes ra) {
        try {
            if (user == null || user.getUniqId() == null) {
                return "redirect:/login";
            }
            if (!newPassword.equals(confirmPassword)) {
                ra.addFlashAttribute("pwErrorMsg", "새 비밀번호가 일치하지 않습니다.");
                return "redirect:/mypage";
            }
            if (newPassword.length() < 4) {
                ra.addFlashAttribute("pwErrorMsg", "비밀번호는 4자 이상이어야 합니다.");
                return "redirect:/mypage";
            }

            MberManageVO mber = mberManageService.selectMber(user.getUniqId());
            if (mber == null) {
                return "redirect:/login";
            }
            // 현재 비밀번호 검증 (저장값은 이중해시)
            String oldHash = EgovFileScrty.encryptPasswordTwice(oldPassword, mber.getMberId());
            if (!oldHash.equals(mber.getPassword())) {
                ra.addFlashAttribute("pwErrorMsg", "현재 비밀번호가 올바르지 않습니다.");
                return "redirect:/mypage";
            }

            // 새 비밀번호 설정 (updateMber 가 이중해시 처리)
            mber.setPassword(newPassword);
            mberManageService.updateMber(mber);

            ra.addFlashAttribute("pwSuccessMsg", "비밀번호가 변경되었습니다.");
        } catch (Exception e) {
            log.error("비밀번호 변경 오류", e);
            ra.addFlashAttribute("pwErrorMsg", "비밀번호 변경 중 오류가 발생했습니다.");
        }
        return "redirect:/mypage";
    }

    /** 변경된 사용자 정보로 JWT 쿠키를 재발급한다. */
    private void reissueToken(LoginVO user, HttpServletResponse response) {
        String jwtToken = jwtTokenUtil.generateToken(user);
        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, jwtToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofSeconds(EgovJwtTokenUtil.JWT_TOKEN_VALIDITY))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
