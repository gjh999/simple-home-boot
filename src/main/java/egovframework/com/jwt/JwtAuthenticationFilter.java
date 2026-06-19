package egovframework.com.jwt;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import egovframework.com.cmm.LoginVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * fileName       : JwtAuthenticationFilter
 * author         : crlee
 * date           : 2023/06/11
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/06/11        crlee       최초 생성
 * 2026/05/13        PHJ         보안취약점 대응
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private EgovJwtTokenUtil jwtTokenUtil;
    public static final String HEADER_STRING = "Authorization";

    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // 1순위: httpOnly 쿠키에서 토큰 읽기
        String jwtToken = null;
        if (req.getCookies() != null) {
            for (Cookie cookie : req.getCookies()) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }
        // 2순위: Authorization 헤더 (Swagger 등 직접 호출 호환)
        if (jwtToken == null || jwtToken.isBlank()) {
            String header = req.getHeader(HEADER_STRING);
            if (header != null && !header.isBlank()) {
                jwtToken = header.startsWith("Bearer ") ? header.substring(7) : header;
            }
        }

        if (jwtToken == null || jwtToken.isBlank()) {
            chain.doFilter(req, res);
            return;
        }

        try {
            LoginVO loginVO = jwtTokenUtil.getLoginVOFromToken(jwtToken);
            String role = isAdmin(loginVO) ? "ROLE_ADMIN" : "ROLE_USER";

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    loginVO, null, Arrays.asList(new SimpleGrantedAuthority(role))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (InvalidJwtException e) {
            // 토큰은 존재하지만 위조·만료 — 브라우저면 로그인 페이지로, API면 401
            SecurityContextHolder.clearContext();
            String acceptHeader = req.getHeader("Accept");
            if (acceptHeader != null && acceptHeader.contains("text/html")) {
                res.sendRedirect(req.getContextPath() + "/login");
            } else {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");
                res.getWriter().write("{\"resultCode\":\"401\",\"resultMessage\":\"invalid or expired token\"}");
            }
            return;
        }

        chain.doFilter(req, res);
    }

    private boolean isAdmin(LoginVO loginVO) {
        return "ROLE_ADMIN".equals(loginVO.getGroupNm());
    }
}
