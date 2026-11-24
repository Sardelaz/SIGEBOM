package com.example.sigebom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/estilo.css", "/equipes.css", "/css/**", "/img/**", "/logo.png", "/favicon.ico", "/esqueci-senha").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/painel", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .sessionManagement(session -> session
                .invalidSessionStrategy((request, response) -> {
                    String uri = request.getRequestURI();
                    String contextPath = request.getContextPath();
                    String queryString = request.getQueryString();
                    request.getSession(true);

                    if (uri.equals(contextPath + "/login")
                            || uri.equals(contextPath + "/esqueci-senha")
                            || uri.endsWith(".css")
                            || uri.endsWith(".png")
                            || uri.endsWith(".ico")) {
                        String destino = (queryString != null && !queryString.isBlank())
                                ? uri + "?" + queryString
                                : uri;
                        response.sendRedirect(destino);
                        return;
                    }
                    response.sendRedirect(contextPath + "/login?expirada=true");
                })
                .maximumSessions(1)
                .expiredUrl("/login?expirada=true")
            );

        return http.build();
    }
}