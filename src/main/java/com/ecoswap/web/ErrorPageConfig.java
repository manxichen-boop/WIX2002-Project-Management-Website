package com.ecoswap.web;

import java.util.Map;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

/** Maps HTTP errors on page requests to the themed templates in templates/pages/error. */
@Configuration
public class ErrorPageConfig {

    @Bean
    ErrorViewResolver themedErrorViews() {
        return (request, status, model) -> {
            String view = switch (status) {
                case FORBIDDEN -> "pages/error/403";
                case NOT_FOUND, METHOD_NOT_ALLOWED -> "pages/error/404";
                default -> status.is4xxClientError() ? "pages/error/404" : "pages/error/500";
            };
            return new ModelAndView(view, Map.of("status", status.value()), status == HttpStatus.METHOD_NOT_ALLOWED ? HttpStatus.NOT_FOUND : status);
        };
    }
}
