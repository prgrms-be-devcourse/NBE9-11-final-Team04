package com.team04.global.config.security;

import com.team04.global.security.CsrfCookieFilter;
import com.team04.global.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.DispatcherType;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "https://seedlink.site",
                "https://www.seedlink.site"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()
//                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
//                        .ignoringRequestMatchers("/auth/**")

                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/users/me/business").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll() // 헬스체크
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/payments/webhooks/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/pre-settlements/*/complete").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/pre-settlements/*/fail").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/refunds/*/complete").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/refunds/*/fail").permitAll()
                        .requestMatchers(HttpMethod.GET, "/payments/config").permitAll()
                        .requestMatchers(HttpMethod.GET, "/fundings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/fundings/{fundingId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/fundings/{fundingId}/sse").permitAll()
                        .requestMatchers(HttpMethod.GET, "/ideas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/ideas/top5").permitAll()
                        .requestMatchers(HttpMethod.GET, "/ideas/{ideaId}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/ideas/bookmarks").authenticated()
                        .requestMatchers(HttpMethod.POST, "/ideas/{ideaId}/bookmark").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/ideas/{ideaId}/bookmark").authenticated()
                        .requestMatchers(HttpMethod.POST, "/ideas/{ideaId}/reports").authenticated()
                        .requestMatchers("/ideas/**").hasRole("USER")
                        .requestMatchers("/workspaces/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/experts/{expertId}").authenticated()
                        .requestMatchers("/experts/verify").authenticated()
                        .requestMatchers(HttpMethod.POST, "/matches/experts/{expertProfileId}").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/experts").authenticated()
                        .requestMatchers("/experts/**").hasRole("EXPERT")
                        .requestMatchers("/matches/**").hasRole("EXPERT")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }
}
