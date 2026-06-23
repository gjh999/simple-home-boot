package egovframework.let.uss.umt.web;

import java.time.Duration;

import egovframework.com.cmm.LoginVO;
import egovframework.com.jwt.EgovJwtTokenUtil;
import egovframework.let.uat.uia.service.EgovLoginService;
import egovframework.let.uss.umt.service.EgovMberManageService;
import egovframework.let.uss.umt.service.MberManageVO;
import egovframework.let.utl.sim.service.EgovFileScrty;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf 기반 회원가입 컨트롤러 (사용자단 셀프 가입)
 *
 * <p>가입 시 {@code EgovMberManageService#insertMber} 를 통해 TB_EMPLYR_INFO 에
 * ROLE_USER 권한·상태 'P'(로그인 가능)로 등록되며, 비밀번호는 이중 SHA-256 해시로 저장되어
 * 기존 Thymeleaf 로그인(/login)과 동일하게 인증된다.</p>
 */
@Slf4j
@Controller
public class EgovMemberRegisterController {

    /** ROLE_USER 권한 그룹 ID (TB_AUTHOR_GROUP_INFO) */
    private static final String ROLE_USER_GROUP_ID = "GROUP_00000000000001";
    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

    @Resource(name = "mberManageService")
    private EgovMberManageService mberManageService;

    @org.springframework.beans.factory.annotation.Autowired
    private egovframework.com.cmm.util.MessageUtil messageUtil;


    @Resource(name = "loginService")
    private EgovLoginService loginService;

    @Autowired
    private EgovJwtTokenUtil jwtTokenUtil;

    @Value("${Globals.jwt.cookieSecure:false}")
    private boolean cookieSecure;

    /**
     * 회원가입 화면
     */
    @GetMapping("/register")
    public String registerView() {
        return "let/uss/umt/registerView";
    }

    /**
     * 회원가입 처리
     */
    @PostMapping("/register")
    public String register(@RequestParam("mberId") String mberId,
                           @RequestParam("password") String password,
                           @RequestParam("passwordConfirm") String passwordConfirm,
                           @RequestParam("mberNm") String mberNm,
                           @RequestParam(value = "mberEmailAdres", required = false) String mberEmailAdres,
                           HttpServletResponse response,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        // 입력값 보존 (오류 시 폼 재표시용)
        model.addAttribute("mberId", mberId);
        model.addAttribute("mberNm", mberNm);
        model.addAttribute("mberEmailAdres", mberEmailAdres);

        // 1. 필수값 검증
        if (isBlank(mberId) || isBlank(password) || isBlank(mberNm)) {
            model.addAttribute("errorMsg", messageUtil.get("msg.register.required"));
            return "let/uss/umt/registerView";
        }
        // 2. 비밀번호 확인
        if (!password.equals(passwordConfirm)) {
            model.addAttribute("errorMsg", messageUtil.get("msg.register.pw.mismatch"));
            return "let/uss/umt/registerView";
        }
        if (password.length() < 4) {
            model.addAttribute("errorMsg", messageUtil.get("msg.register.pw.tooShort"));
            return "let/uss/umt/registerView";
        }

        try {
            // 3. 아이디 중복 확인 (EMPLYR_ID 기준 카운트)
            if (mberManageService.checkIdDplct(mberId) > 0) {
                model.addAttribute("errorMsg", messageUtil.get("msg.register.id.dup"));
                return "let/uss/umt/registerView";
            }

            // 4. 회원 등록 (서비스에서 uniqId 생성 + 비밀번호 이중해시 처리)
            MberManageVO vo = new MberManageVO();
            vo.setMberId(mberId);
            vo.setPassword(password);
            vo.setMberNm(mberNm);
            vo.setMberEmailAdres(mberEmailAdres == null ? "" : mberEmailAdres);
            vo.setMberSttus("P");               // 로그인 가능 상태
            vo.setGroupId(ROLE_USER_GROUP_ID);  // 일반 사용자 권한

            mberManageService.insertMber(vo);

            // 5. 가입 직후 자동 로그인 — JWT 쿠키 발급 후 포털로 이동
            //    (로그인 컨트롤러와 동일한 방식: 평문 비밀번호를 1차 해시 → 서비스가 2차 해시 비교)
            LoginVO loginVO = new LoginVO();
            loginVO.setId(mberId);
            loginVO.setUserSe("USR");
            loginVO.setPassword(EgovFileScrty.encryptPassword(password, mberId));
            LoginVO result = loginService.actionLogin(loginVO);

            if (result != null && result.getId() != null && !result.getId().isEmpty()) {
                if ("ROLE_ADMIN".equals(result.getGroupNm())) {
                    result.setUserSe("ADM");
                }
                String jwtToken = jwtTokenUtil.generateToken(result);
                ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, jwtToken)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Strict")
                        .path("/")
                        .maxAge(Duration.ofSeconds(EgovJwtTokenUtil.JWT_TOKEN_VALIDITY))
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                return "redirect:/portal";
            }

            // 자동 로그인 실패(예외적) 시 로그인 화면으로 안내
            redirectAttributes.addAttribute("registered", "true");
            return "redirect:/login";

        } catch (Exception e) {
            log.error("회원가입 처리 중 오류", e);
            model.addAttribute("errorMsg", messageUtil.get("msg.register.error"));
            return "let/uss/umt/registerView";
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
