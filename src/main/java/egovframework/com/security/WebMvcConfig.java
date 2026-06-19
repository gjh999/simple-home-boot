package egovframework.com.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import egovframework.com.config.HtmlCharacterEscapes;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;

/**
 * fileName       : WebMvcConfig
 * author         : crlee
 * date           : 2023/07/13
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/13        crlee       최초 생성
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
	
	private final ObjectMapper objectMapper;
	
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new CustomAuthenticationPrincipalResolver());
    }

    /**
     * 다국어 로케일 해석기 — STATELESS 환경이므로 쿠키(LANG)에 선택 언어를 저장한다.
     * 기본 언어는 한국어.
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("LANG");
        resolver.setDefaultLocale(Locale.KOREAN);
        resolver.setCookiePath("/");
        resolver.setCookieMaxAge(java.time.Duration.ofDays(365));
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 동적(컨트롤러) 응답은 브라우저가 캐시하지 못하도록 한다.
        // 게시물 등록 후 목록이 새로고침해야 보이던 문제와, 언어 전환이 한 번에 반영되지 않던
        // 문제(이전 언어 페이지가 캐시에서 응답)를 방지한다.
        // 정적 리소스와 바이너리 응답(파일/이미지 다운로드)은 캐시 가능하도록 제외한다.
        // 언어 전환은 EgovLangController(/cmm/lang)가 LocaleResolver 로 직접 처리하므로
        // 별도의 LocaleChangeInterceptor 는 두지 않는다(중복 적용 방지).
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setDateHeader("Expires", 0);
                return true;
            }
        }).excludePathPatterns("/css/**", "/js/**", "/images/**", "/fonts/**",
                "/static/**", "/favicon.ico", "/webjars/**", "/swagger-ui/**", "/v3/api-docs/**",
                "/file", "/image");
    }
    
    @Bean
    public HttpMessageConverter<?> htmlEscapingConverter() {
        ObjectMapper copy = objectMapper.copy();
        copy.getFactory().setCharacterEscapes(new HtmlCharacterEscapes());
        return new MappingJackson2HttpMessageConverter(copy);
    }
    
}