package com.hoaiphong.quanlysanpham.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class LocaleResolver extends AcceptHeaderLocaleResolver implements WebMvcConfigurer {

    List<Locale> LOCALES = List.of(new Locale("en"),new Locale("vi"));

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String languageHeader = request.getHeader("Accept-Language");
        return !StringUtils.hasLength(languageHeader)
                ? Locale.US
                : Locale.lookup(Locale.LanguageRange.parse(languageHeader), LOCALES);
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource rs = new ReloadableResourceBundleMessageSource();
        rs.setBasename("classpath:messages");
        rs.setDefaultEncoding("UTF-8");
        rs.setCacheSeconds(3600);
        rs.setUseCodeAsDefaultMessage(true);
        return rs;
    }
}