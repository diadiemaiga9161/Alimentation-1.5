package com.ges.boutique.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class MobileController {

    @GetMapping(
        value = {
            "/mobile",
            "/mobile/",
            "/mobile/{path:[^\\.]*}",
            "/mobile/{p1:[^\\.]*}/{path:[^\\.]*}",
            "/mobile/{p1:[^\\.]*}/{p2:[^\\.]*}/{path:[^\\.]*}"
        },
        produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<String> serveIonicApp() {
        try {
            ClassPathResource resource = new ClassPathResource("static/mobile/index.html");
            String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><h2>Application mobile non disponible.</h2></body></html>");
        }
    }
}
