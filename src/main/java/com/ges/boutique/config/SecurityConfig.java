package com.ges.boutique.config;

import com.ges.boutique.securite.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // ====================
                        // FICHIERS STATIQUES
                        // ====================
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",

                                "/*.css",
                                "/*.js",
                                "/*.png",
                                "/*.jpg",
                                "/*.jpeg",
                                "/*.gif",
                                "/*.svg",
                                "/*.ico",
                                "/*.woff",
                                "/*.woff2",
                                "/*.ttf",
                                "/*.eot",
                                "/*.otf",
                                "/*.json",
                                "/*.html",

                                "/assets/**",
                                "/static/**",
                                "/resources/**",
                                "/public/**",
                                "/content/**",
                                "/scripts/**",
                                "/styles/**",
                                "/images/**",
                                "/icons/**",
                                "/fonts/**",
                                "/vendor/**"
                        ).permitAll()

                        // ====================
                        // ROUTES FRONTEND SPA
                        // ====================
                        .requestMatchers(
                                "/home",
                                "/dashboard",
                                "/login",
                                "/register",
                                "/connexion",
                                "/inscription",
                                "/produit",
                                "/client",
                                "/commande",
                                "/categorie",
                                "/vente",
                                "/inventaire",
                                "/rapport",
                                "/profile",
                                "/settings",

                                "/sessions/**",
                                "/produit/**",
                                "/client/**",
                                "/commande/**",
                                "/categorie/**",
                                "/vente/**",
                                "/inventaire/**",
                                "/rapport/**",
                                "/pages/**",
                                "/others/**",
                                "/error/**"
                        ).permitAll()

                        // ====================
                        // API PUBLIQUES
                        // ====================
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/test/**",
                                "/api/public/**"
                        ).permitAll()

                        // ====================
                        // DOCUMENTATION API
                        // ====================
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // ====================
                        // ROUTES PROTÉGÉES
                        // ====================
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/rapports/**").hasRole("ADMIN")

                        .requestMatchers(
                                "/api/produits/**",
                                "/api/ventes/**",
                                "/api/inventaire/**",
                                "/api/clients/**",
                                "/api/commandes/**",
                                "/api/categories/**"
                        ).hasAnyRole("ADMIN", "VENDEUR")

                        // ====================
                        // AUTRES API → AUTH
                        // ====================
                        .requestMatchers("/api/**").authenticated()

                        // ====================
                        // RESTE → LIBRE
                        // ====================
                        .anyRequest().permitAll()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:8080"
        ));

        configuration.setAllowedMethods(List.of(
                "GET","POST","PUT","DELETE","OPTIONS","PATCH","HEAD"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization","Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
