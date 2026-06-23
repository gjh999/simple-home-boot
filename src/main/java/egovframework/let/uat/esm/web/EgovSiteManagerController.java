package egovframework.let.uat.esm.web;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import egovframework.com.cmm.LoginVO;
import egovframework.let.uat.esm.service.EgovSiteManagerService;
import egovframework.let.utl.sim.service.EgovFileScrty;

/**
 * Thymeleaf 기반 사이트관리자 비밀번호 변경 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/admin")
public class EgovSiteManagerController {

    @Resource(name = "siteManagerService")
    private EgovSiteManagerService siteManagerService;

    @org.springframework.beans.factory.annotation.Autowired
    private egovframework.com.cmm.util.MessageUtil messageUtil;


    /**
     * 관리자 비밀번호 변경 폼
     */
    @GetMapping("/password")
    public String passwordForm(Model model) {
        return "let/uat/esm/adminPassword";
    }

    /**
     * 관리자 비밀번호 변경 처리
     */
    @PostMapping("/password")
    public String passwordProcess(@RequestParam("oldPassword") String oldPassword,
                                  @RequestParam("newPassword") String newPassword,
                                  @RequestParam("confirmPassword") String confirmPassword,
                                  @AuthenticationPrincipal LoginVO loginVO,
                                  RedirectAttributes ra) {
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("errorMsg", messageUtil.get("msg.adminpw.mismatch"));
            return "redirect:/admin/password";
        }

        try {
            String loginId = loginVO.getId();
            Map<String, Object> param = new HashMap<>();
            param.put("old_password", EgovFileScrty.encryptPasswordTwice(oldPassword, loginId));
            param.put("new_password", EgovFileScrty.encryptPasswordTwice(newPassword, loginId));
            param.put("login_id", loginId);

            Integer result = siteManagerService.updateAdminPassword(param);
            if (result != null && result > 0) {
                ra.addFlashAttribute("successMsg", messageUtil.get("msg.adminpw.changed"));
            } else {
                ra.addFlashAttribute("errorMsg", messageUtil.get("msg.adminpw.curWrong"));
            }
        } catch (Exception e) {
            log.error("비밀번호 변경 오류", e);
            ra.addFlashAttribute("errorMsg", messageUtil.get("msg.adminpw.error"));
        }
        return "redirect:/admin/password";
    }
}
