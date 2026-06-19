package egovframework.com.jwt;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import egovframework.com.cmm.ResponseCode;
import egovframework.com.cmm.service.ResultVO;

/**
 * fileName       : JwtAuthenticationEntryPoint
 * author         : crlee
 * date           : 2023/06/11
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/06/11        crlee       최초 생성
 */

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {


    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {

        String acceptHeader = request.getHeader("Accept");
        // 브라우저 요청이면 로그인 페이지로 리다이렉트
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        ResultVO resultVO = new ResultVO();
        resultVO.setResultCode(ResponseCode.AUTH_ERROR.getCode());
        resultVO.setResultMessage(ResponseCode.AUTH_ERROR.getMessage());
        ObjectMapper mapper = new ObjectMapper();

        String jsonInString = mapper.writeValueAsString(resultVO);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON.toString());
        response.setCharacterEncoding("UTF-8");
        response.getWriter().println(jsonInString);
    }
}