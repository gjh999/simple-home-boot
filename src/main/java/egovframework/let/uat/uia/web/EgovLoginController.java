package egovframework.let.uat.uia.web;

import java.time.Duration;

import egovframework.com.cmm.LoginVO;
import egovframework.com.jwt.EgovJwtTokenUtil;
import egovframework.let.uat.uia.service.EgovLoginService;
import egovframework.let.utl.sim.service.EgovFileScrty;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Thymeleaf 기반 로그인/로그아웃 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class EgovLoginController {

    private final EgovLoginService loginService;
    private final EgovJwtTokenUtil jwtTokenUtil;
    private final egovframework.com.cmm.util.MessageUtil messageUtil;

    @Value("${Globals.jwt.cookieSecure:false}")
    private boolean cookieSecure;

    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

    /**
     * 로그인 화면
     */
    @GetMapping("/login")
    public String loginView(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", messageUtil.get("msg.login.fail"));
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", messageUtil.get("msg.logout.done"));
        }
        return "let/uat/uia/loginView";
    }

    /**
     * 로그인 처리 (Thymeleaf 폼 POST)
     * 평문 비밀번호를 받아 서버에서 1차 해시 후 loginService 호출
     */
    @PostMapping("/login")
    public String loginProcess(@RequestParam("id") String id,
                               @RequestParam("password") String password,
                               HttpServletResponse response,
                               RedirectAttributes redirectAttributes) {
        try {
            LoginVO loginVO = new LoginVO();
            loginVO.setId(id);
            loginVO.setUserSe("USR"); // 임직원(업무사용자) 기본값: LETTNEMPLYRINFO 조회
            // Thymeleaf 폼은 평문 비밀번호를 전송 — 서버에서 1차 해시 처리
            String firstHash = EgovFileScrty.encryptPassword(password, id);
            loginVO.setPassword(firstHash);

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
            } else {
                redirectAttributes.addAttribute("error", "true");
                return "redirect:/login";
            }
        } catch (Exception e) {
            log.error("로그인 처리 중 오류", e);
            redirectAttributes.addAttribute("error", "true");
            return "redirect:/login";
        }
    }

    /**
     * 로그아웃 처리
     */
    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        ResponseCookie expiredCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
        return "redirect:/login?logout=true";
    }
}
