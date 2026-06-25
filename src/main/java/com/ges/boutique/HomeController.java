package com.ges.boutique;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Sert index.html pour toutes les routes Angular (SPA deep links)
    // Le pattern [^\\.]*  exclut les chemins avec extension (fichiers statiques)
    @GetMapping(value = {
        "/",
        "/{path:(?!ws$)[^\\.]*}",
        "/{p1:(?!ws$)[^\\.]*}/{path:[^\\.]*}",
        "/{p1:(?!ws$)[^\\.]*}/{p2:[^\\.]*}/{path:[^\\.]*}",
        "/{p1:(?!ws$)[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}/{path:[^\\.]*}"
    })
    public String index() {
        return "forward:/index.html";
    }
}