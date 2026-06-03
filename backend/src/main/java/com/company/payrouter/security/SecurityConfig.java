package com.company.payrouter.security;

import com.company.payrouter.common.api.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health",
                                "/api/docs",
                                "/api/docs/**",
                                "/api/pay/**",
                                "/api/system-settings",
                                "/uploads/**",
                                "/actuator/**",
                                "/doc.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/admin/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin/api-docs", "/admin/api-docs/**").hasAuthority("dashboard:view")
                        .requestMatchers("/admin/dashboard/**").hasAuthority("dashboard:view")
                        .requestMatchers(HttpMethod.GET, "/admin/permissions/tree").hasAuthority("system:role:view")
                        .requestMatchers(HttpMethod.GET, "/admin/system-settings").hasAuthority("system:settings:view")
                        .requestMatchers("/admin/system-settings/**", "/admin/system-settings").hasAuthority("system:settings:manage")
                        .requestMatchers(HttpMethod.GET, "/admin/users/**").hasAuthority("system:user:view")
                        .requestMatchers("/admin/users/**").hasAuthority("system:user:manage")
                        .requestMatchers(HttpMethod.GET, "/admin/roles/**").hasAuthority("system:role:view")
                        .requestMatchers("/admin/roles/**").hasAuthority("system:role:manage")
                        .requestMatchers(HttpMethod.GET, "/admin/merchant-pools/**").hasAuthority("merchant:pool:view")
                        .requestMatchers("/admin/merchant-pools/**").hasAuthority("merchant:pool:manage")
                        .requestMatchers(HttpMethod.GET, "/admin/merchant-accounts/**").hasAuthority("merchant:account:view")
                        .requestMatchers("/admin/merchant-accounts/**").hasAuthority("merchant:account:manage")
                        .requestMatchers(HttpMethod.GET, "/admin/merchant-apps/**").hasAuthority("merchant:app:view")
                        .requestMatchers("/admin/merchant-apps/**").hasAuthority("merchant:app:manage")
                        .requestMatchers(HttpMethod.GET, "/admin/pay-methods/**").hasAuthority("paymethod:view")
                        .requestMatchers("/admin/pay-methods/**").hasAuthority("paymethod:manage")
                        .requestMatchers(HttpMethod.GET, "/admin/route-rules/**").hasAuthority("route:rule:view")
                        .requestMatchers("/admin/route-rules/**").hasAuthority("route:rule:manage")
                        .requestMatchers("/admin/route/test", "/admin/route/pay-test", "/admin/route/query-test").hasAuthority("route:test")
                        .requestMatchers(HttpMethod.GET, "/admin/route-records").hasAuthority("route:record:view")
                        .requestMatchers(HttpMethod.GET, "/admin/orders/**").hasAuthority("order:view")
                        .requestMatchers("/admin/orders/**").hasAuthority("order:manage")
                        .requestMatchers(HttpMethod.GET, "/admin/refunds/**").hasAuthority("refund:view")
                        .requestMatchers("/admin/refunds/**").hasAuthority("refund:manage")
                        .requestMatchers(HttpMethod.GET, "/admin/order-logs/**", "/admin/notify-logs/**").hasAuthority("order:log:view")
                        .requestMatchers("/admin/**").authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> writeError(response, 401, "Unauthorized"))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeError(response, 403, "Forbidden"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response, int code, String message) throws java.io.IOException {
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.failure(code, message)));
    }
}
