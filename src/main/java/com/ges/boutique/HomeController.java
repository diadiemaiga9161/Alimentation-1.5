package com.ges.boutique;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Version simple - laisse Spring Boot gérer les fichiers statiques
     * et ne redirige que les routes principales
     */
    @GetMapping({"/", "/home"})
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping({
            "/login", "/register", "/connexion", "/inscription",
            "/dashboard", "/profile", "/settings"
    })
    public String authRoutes() {
        return "forward:/index.html";
    }

    @GetMapping({
            "/produit", "/client", "/commande", "/categorie",
            "/vente", "/inventaire", "/rapport", "/pages"
    })
    public String appRoutes() {
        return "forward:/index.html";
    }
}