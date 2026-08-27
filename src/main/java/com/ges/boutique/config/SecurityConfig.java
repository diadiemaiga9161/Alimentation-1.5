package com.ges.boutique.config;

import com.ges.boutique.securite.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Value("${cors.allowed-origins:*}")
    private String allowedOriginsStr;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configure(http))

                .authorizeHttpRequests(auth -> auth

                        // CORS preflight — toujours en premier
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // WebSocket STOMP
                        .requestMatchers("/ws/**").permitAll()

                        // Sync batch offline (clients mobiles — ping + batch)
                        .requestMatchers("/api/sync/ping").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sync/batch").authenticated()

                        // Auth publique
                        .requestMatchers("/api/auth/**", "/api/login", "/api/register").permitAll()

                        // Transfert inter-boutiques — appelé par RestTemplate sans token
                        .requestMatchers(HttpMethod.POST, "/api/transferts/recevoir").permitAll()

                        // PDF + QR code facture — accessibles via scan QR code sans authentification
                        .requestMatchers(HttpMethod.GET, "/api/caisse/factures/*/pdf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/caisse/factures/*/pdf/view").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/caisse/factures/*/qrcode").permitAll()

                        // Facture publique — scan QR code depuis téléphone client (sans auth)
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                        // Relevé client PDF + QR code — public (client scanne sans être connecté)
                        .requestMatchers(HttpMethod.GET, "/api/clients/*/releve-pdf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clients/*/qrcode").permitAll()

                        // Mini-site vitrine automatique — catalogue produits + infos boutique, public (sans connexion)
                        .requestMatchers("/api/vitrine/**").permitAll()

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/api-docs/**", "/v3/api-docs/**"
                        ).permitAll()

                        // Ventes — rôles spécifiques
                        .requestMatchers(HttpMethod.GET, "/api/ventes/**").hasAnyRole("ADMIN", "VENDEUR")
                        .requestMatchers(HttpMethod.POST, "/api/ventes/**").hasAnyRole("ADMIN", "VENDEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/ventes/**").hasAnyRole("ADMIN", "VENDEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/ventes/*/modifier-lignes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/ventes/**").hasRole("ADMIN")
                        .requestMatchers("/api/retours-ventes/**").hasAnyRole("ADMIN", "VENDEUR")

                        // Journal d'audit — consultation réservée aux admins
                        .requestMatchers("/api/journal-audit/**").hasRole("ADMIN")

                        // Sauvegarde automatique / manuelle de la base — réservée aux admins
                        .requestMatchers("/api/backup/**").hasRole("ADMIN")

                        // Toutes les autres routes API → authentification obligatoire
                        .requestMatchers("/api/**").authenticated()

                        // Tout le reste = fichiers Angular/Ionic (JS, CSS, HTML, assets...) → public
                        .anyRequest().permitAll()
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Lit depuis application-boutique*.properties (cors.allowed-origins)
        // allowedOriginPatterns("*") accepte tous les origines + fonctionne avec credentials
        List<String> origins = Arrays.asList(allowedOriginsStr.split(","));
        config.setAllowedOriginPatterns(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
