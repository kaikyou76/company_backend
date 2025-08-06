package com.example.companybackend.config;

import com.example.companybackend.security.JwtAuthenticationEntryPoint;
import com.example.companybackend.security.JwtAuthenticationFilter;
import com.example.companybackend.security.XssProtectionFilter;
import com.example.companybackend.security.HtmlSanitizerService;
import com.example.companybackend.security.JwtTokenProvider;
import com.example.companybackend.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

/**
 * 統合版セキュリティ設定
 * Spring Security 6.x & Spring Boot 3.x 対応
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomUserDetailsService customUserDetailsService;
    
    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                         JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                         CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 启用JWT认证
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // 允许访问认证相关端点
                .requestMatchers("/api/auth/**").permitAll()
                // 允许访问用户注册端点
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                // 允许访问CSRFトークン端点
                .requestMatchers("/api/csrf-token").permitAll()
                // 允许访问Swagger UI和API文档相关资源
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            // 禁用表单登录和HTTP基本认证
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // 启用CSRF保護（使用Spring Security默认机制）
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            // 禁用重定向到登录页面
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new Http403ForbiddenEntryPoint()));

        // 添加JWT认证过滤器
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        
        // 添加XSS防护过滤器
        http.addFilterAfter(xssProtectionFilter(), CsrfFilter.class);

        // 配置安全头
        http.headers(headers -> headers
                .frameOptions().sameOrigin()
                .contentTypeOptions()
                .and()
                .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;")
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public XssProtectionFilter xssProtectionFilter() {
        return new XssProtectionFilter(new HtmlSanitizerService());
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService);
    }
}