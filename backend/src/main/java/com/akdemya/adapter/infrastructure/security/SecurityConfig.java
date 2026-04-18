package com.akdemya.adapter.infrastructure.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwt;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${app.rate-limit.login.requests-per-minute:5}")
    private int loginRateLimit;

    @Value("${app.rate-limit.register.requests-per-minute:3}")
    private int registerRateLimit;

    public SecurityConfig(JwtService jwt,
                          GoogleOAuth2UserService googleOAuth2UserService,
                          OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.jwt = jwt;
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // IF_REQUIRED: stateless for JWT requests, creates a session only
            // during the OAuth2 authorization code flow to store the state param.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(reg -> reg
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                // OAuth2 endpoints are served under /api to go through the single nginx proxy rule
                .requestMatchers("/api/oauth2/**", "/api/login/oauth2/**").permitAll()
                .requestMatchers("/api/manage/**").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                // Mount OAuth2 endpoints under /api so they share the same
                // nginx proxy rule as regular API calls (/api/ → backend:8080).
                // This prevents the SPA from intercepting /oauth2/authorization/google
                // and redirecting to home (React Router catch-all).
                .authorizationEndpoint(auth -> auth
                    .baseUri("/api/oauth2/authorization"))
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/api/login/oauth2/code/*"))
                .userInfoEndpoint(ui -> ui.userService(googleOAuth2UserService))
                .successHandler(oAuth2SuccessHandler));

        http.addFilterBefore(new RateLimitFilter(loginRateLimit, registerRateLimit),
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        http.addFilterBefore(new JwtAuthFilter(jwt),
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "Cookie"));
        cfg.setExposedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // NOTE: PasswordEncoder @Bean lives in PasswordEncoderConfig to break the
    // circular dependency chain:
    //   SecurityConfig → OAuth2SuccessHandler → AuthManager
    //     → SpringSecurityPasswordHasher → PasswordEncoder (@Bean here → cycle)

    static class JwtAuthFilter extends OncePerRequestFilter {
        private final JwtService jwt;

        JwtAuthFilter(JwtService jwt) { this.jwt = jwt; }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String token = resolveToken(req);
            if (token != null) {
                try {
                    var jws = jwt.parse(token);
                    String email = jws.getBody().getSubject();
                    String role  = String.valueOf(jws.getBody().get("role"));
                    UserDetails principal = User.withUsername(email)
                        .password("").authorities("ROLE_" + role).build();
                    var auth = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(JwtAuthFilter.class)
                        .warn("JWT validation failed: {}", e.getMessage());
                }
            }
            chain.doFilter(req, res);
        }

        private String resolveToken(HttpServletRequest req) {
            String header = req.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                return Arrays.stream(cookies)
                    .filter(c -> "ak_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(v -> v != null && !v.isBlank())
                    .findFirst()
                    .orElse(null);
            }
            return null;
        }
    }
}
