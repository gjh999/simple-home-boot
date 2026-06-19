package egovframework.let.main.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 회사(센터) 소개 랜딩 페이지 컨트롤러
 *
 * <p>전자정부표준프레임워크 경량환경을 소개하는 모던 랜딩 페이지를 제공한다.
 * 기존 메인 화면(/)과 별개로 동작하며, 스크롤 애니메이션이 적용된 단일 페이지로 구성된다.</p>
 */
@Slf4j
@Controller
public class EgovLandingController {

    /**
     * 회사 소개 랜딩 페이지 (메인 진입점)
     */
    @GetMapping({"/", "/landing"})
    public String landing() {
        return "let/main/landingView";
    }
}
