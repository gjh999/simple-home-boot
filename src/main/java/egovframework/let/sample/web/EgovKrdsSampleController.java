package egovframework.let.sample.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * KRDS(디지털정부 표준 디자인시스템) 사용 예시 페이지 컨트롤러.
 *
 * <p>KRDS 네이티브 클래스(`krds-*`)만으로 구성한 참고용 페이지를 제공한다.
 * 게시판 목록(KRDS 표)·탭·아코디언·버튼·배지·폼·브레드크럼·페이지네이션 등
 * 실제 KRDS 마크업 사용법을 한 화면에서 확인할 수 있으며, 다른 페이지를
 * KRDS 네이티브로 전환할 때의 표준 패턴 역할을 한다.</p>
 */
@Slf4j
@Controller
public class EgovKrdsSampleController {

    /** KRDS 컴포넌트 사용 예시(보드리스트 포함) */
    @GetMapping("/krds-sample")
    public String krdsSample() {
        return "let/sample/krdsSample";
    }
}
