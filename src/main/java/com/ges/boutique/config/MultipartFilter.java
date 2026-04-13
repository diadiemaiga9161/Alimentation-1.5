package com.ges.boutique.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MultipartFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String contentType = httpRequest.getContentType();

        System.out.println("🔍 Filter: Content-Type: " + contentType);
        System.out.println("🔍 Filter: Méthode: " + httpRequest.getMethod());
        System.out.println("🔍 Filter: URI: " + httpRequest.getRequestURI());

        // Si c'est une requête multipart/form-data
        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            System.out.println("✅ Filter: Requête multipart/form-data détectée");
        }

        chain.doFilter(request, response);
    }
}