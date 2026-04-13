package com.ges.boutique.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir les ressources statiques depuis classpath:/static/
        registry.addResourceHandler(
                "/**/*.css",
                "/**/*.js",
                "/**/*.png",
                "/**/*.jpg",
                "/**/*.jpeg",
                "/**/*.gif",
                "/**/*.svg",
                "/**/*.ico",
                "/**/*.ttf",
                "/**/*.woff",
                "/**/*.woff2",
                "/**/*.eot",
                "/**/*.otf",
                "/**/*.json",
                "/assets/**",
                "/static/**",
                "/resources/**",
                "/public/**"
        ).addResourceLocations("classpath:/static/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Rediriger la racine vers index.html
        registry.addViewController("/").setViewName("forward:/index.html");

        // Pour les routes SPA - CORRECTION ICI : plus de /**/{spring:[^.]*}
        // Cette configuration permet de servir index.html pour toutes les routes
        // qui ne correspondent pas à des fichiers avec extension
        registry.addViewController("/{path:[^\\.]*}").setViewName("forward:/index.html");
    }
}