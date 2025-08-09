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
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * 統合版セキュリティ設定
 * Spring Security 6.x & Spring Boot 3.x 対応
 * CSRF保護とCORS設定を含む
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomUserDetailsService customUserDetailsService;
    private final Environment environment;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            CustomUserDetailsService customUserDetailsService,
            Environment environment) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.customUserDetailsService = customUserDetailsService;
        this.environment = environment;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CORS設定を有効化
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // JWT認証設定
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 認証関連エンドポイントを許可
                        .requestMatchers("/api/auth/**").permitAll()
                        // ユーザー登録エンドポイントを許可
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // CSRFトークンエンドポイントを許可
                        .requestMatchers("/api/csrf/**").permitAll()
                        // Swagger UI関連リソースを許可
                        .requestMatchers("/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        // その他のリクエストは認証が必要
                        .anyRequest().authenticated())
                // フォームログインとHTTP基本認証を無効化
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // CSRF保護設定（Spring Security標準設定）
                .csrf(csrf -> {
                    // CSRFトークンリポジトリの設定
                    CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                    tokenRepository.setCookieName("XSRF-TOKEN");
                    tokenRepository.setHeaderName("X-XSRF-TOKEN");

                    // CSRFトークンリクエストハンドラーの設定
                    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                    requestHandler.setCsrfRequestAttributeName("_csrf");

                    csrf.csrfTokenRepository(tokenRepository)
                            .csrfTokenRequestHandler(requestHandler)
                            // CSRFトークン取得エンドポイントは除外
                            .ignoringRequestMatchers("/api/csrf/**");
                })
                // 認証エラー時の処理
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new Http403ForbiddenEntryPoint()));

        // JWT認証フィルターを追加
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        // XSS防護フィルターを追加
        http.addFilterAfter(xssProtectionFilter(), CsrfFilter.class);

        // セキュリティヘッダーの設定
        http.headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                .contentTypeOptions(contentTypeOptions -> {
                })
                .referrerPolicy(referrer -> referrer
                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter(
                        "Content-Security-Policy",
                        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; font-src 'self'; object-src 'none'; media-src 'self'; frame-src 'none';"))
                .httpStrictTransportSecurity(hsts -> hsts
                        .maxAgeInSeconds(31536000) // 1年
                        .includeSubDomains(true)
                        .preload(true)));

        return http.build();
    }

    /**
     * CORS設定
     * フロントエンド（localhost:3000）からのリクエストを許可
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // localhost:3000を明示的に許可（クレデンシャル対応のためワイルドカードは使用しない）
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));

        // クレデンシャルを許可
        configuration.setAllowCredentials(true);

        // 基本的なヘッダーを許可
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 基本的なHTTPメソッドを許可
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // フロントエンドが必要とするヘッダーを公開
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-CSRF-TOKEN", "X-XSRF-TOKEN"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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