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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()
//                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
//                        .ignoringRequestMatchers("/auth/**")

                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/users/me/business").hasRole("PROPOSER")
                        .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll() // 헬스체크
                        .requestMatchers("/payments/webhooks/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/fundings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/fundings/{fundingId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/fundings/{fundingId}/sse").permitAll()
                        .requestMatchers(HttpMethod.GET, "/ideas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/experts/{expertId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/matches/experts/{expertProfileId}").hasRole("PROPOSER")
                        .requestMatchers("/experts/**").hasRole("EXPERT")
                        .requestMatchers("/matches/**").hasRole("EXPERT")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }
}
