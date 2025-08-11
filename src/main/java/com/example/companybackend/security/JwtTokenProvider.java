package com.example.companybackend.security;

import com.example.companybackend.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT トークンプロバイダー（統合版）
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private final SecretKey secretKey;
    private final long tokenValidityInMilliseconds;
    private final long refreshExpiration;
    private final JdbcTemplate jdbcTemplate;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:default-secret-key-for-company-system-at-least-32-chars}") String secret,
            @Value("${app.jwt.expiration:86400000}") long tokenValidityInMillis,
            @Value("${app.jwt.refresh-expiration:604800000}") long refreshExpiration,
            JdbcTemplate jdbcTemplate) {

        System.out.println("=== JWT Configuration Debug ===");
        System.out.println("Secret: " + secret);
        System.out.println("Token validity from properties: " + tokenValidityInMillis + " ms");
        System.out.println("Refresh expiration from properties: " + refreshExpiration + " ms");
        
        // 使用更安全の密钥生成方式
        this.secretKey = Keys.hmacShaKeyFor(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded());
        
        // 尝試从数据库获取JWT过期配置，如果获取不到则使用默认値
        long dbTokenValidity = getTokenValidityFromDatabase(jdbcTemplate);
        this.tokenValidityInMilliseconds = dbTokenValidity != -1 ? dbTokenValidity : tokenValidityInMillis;
        
        System.out.println("JWT Token Validity Configuration:");
        System.out.println("  Default value from properties: " + tokenValidityInMillis + " ms");
        System.out.println("  Value from database: " + dbTokenValidity + " ms");
        System.out.println("  Final value used: " + this.tokenValidityInMilliseconds + " ms");
        
        this.refreshExpiration = refreshExpiration;
        this.jdbcTemplate = jdbcTemplate;
        
        System.out.println("=== End JWT Configuration Debug ===");
    }

    /**
     * 从数据库获取JWTトークン有效期
     * @param jdbcTemplate データベースアクセステンプレート
     * @return トークン有效期（ミリ秒），もしそれが見つからなければ-1
     */
    private long getTokenValidityFromDatabase(JdbcTemplate jdbcTemplate) {
        try {
            Long validity = jdbcTemplate.queryForObject(
                "SELECT config_value::bigint FROM security_test_config WHERE config_key = 'jwt.test.expiration' AND is_active = true",
                Long.class
            );
            System.out.println("Successfully retrieved JWT expiration from database: " + validity + " ms");
            return validity != null ? validity : -1;
        } catch (Exception e) {
            log.warn("データベースからJWT有効期限を取得できませんでした。デフォルト値を使用します。エラー: {}", e.getMessage());
            System.out.println("Failed to get JWT expiration from database: " + e.getMessage());
            return -1;
        }
    }

    /**
     * JWT アクセストークン生成（ユーザーオブジェクト直接指定）
     */
    public String createToken(User user) {
        return generateTokenFromUser(user);
    }

    /**
     * JWT アクセストークン生成（認証情報経由）
     */
    public String generateToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return generateTokenFromUser((User) principal);
        } else {
            return generateDefaultToken(authentication);
        }
    }

    /**
     * ユーザーオブジェクトからトークン生成（内部メソッド）
     */
    private String generateTokenFromUser(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("locationType", user.getLocationType())
                .claim("departmentId", user.getDepartmentId())
                .claim("positionId", user.getPositionId())
                .claim("managerId", user.getManagerId())
                .claim("authorities", getAuthoritiesFromUser(user))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * デフォルトトークン生成（内部メソッド）
     */
    private String generateDefaultToken(Authentication authentication) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(authentication.getName())
                .claim("authorities", getAuthorities(authentication))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * リフレッシュトークン生成
     */
    public String generateRefreshToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        if (principal instanceof User) {
            User user = (User) principal;
            return Jwts.builder()
                    .subject(user.getUsername())
                    .claim("userId", user.getId())
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(secretKey)
                    .compact();
        } else {
            return Jwts.builder()
                    .subject(authentication.getName())
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(secretKey)
                    .compact();
        }
    }

    // --- トークン検証・情報取得メソッド ---

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("無効なJWTトークン: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = claims.get("userId", Long.class);
            if (userId != null) {
                return userId;
            }
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("無効なJWTトークン: {}", e.getMessage());
            return null;
        }
    }

    public String getLocationTypeFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("locationType", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("無効なJWTトークン: {}", e.getMessage());
            return null;
        }
    }

    public Integer getDepartmentIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("departmentId", Integer.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("無効なJWTトークン: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            // 修正: Jwts.parserBuilder() を最新版の書き方に合わせる
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.warn("不正なJWTトークン: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("期限切れのJWTトークン: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("サポートされていないJWTトークン: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWTトークンクレームが空です: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWTトークンエラー: {}", e.getMessage());
        }
        return false;
    }

    public Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("無効なJWTトークン: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            if (claims == null)
                return true;
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.warn("トークン期限チェックエラー: {}", e.getMessage());
            return true;
        }
    }

    public long getRemainingValidityTime(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            if (claims == null)
                return 0;
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            log.warn("トークン残り時間計算エラー: {}", e.getMessage());
            return 0;
        }
    }

    public Integer getPositionIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("positionId", Integer.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("無効なJWTトークン: {}", e.getMessage());
            return null;
        }
    }

    public Integer getManagerIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("managerId", Integer.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("無効なJWTトークン: {}", e.getMessage());
            return null;
        }
    }

    public long getTokenValidityInMilliseconds() {
        return tokenValidityInMilliseconds;
    }

    /**
     * シークレットキーを取得する（テスト用）
     *
     * @return SecretKey シークレットキー
     */
    public SecretKey getSecretKey() {
        return this.secretKey;
    }

    /**
     * 権限情報取得（認証情報経由）
     */
    private List<String> getAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    /**
     * 権限情報取得（ユーザーオブジェクト経用）
     */
    private List<String> getAuthoritiesFromUser(User user) {
        // ユーザーの権限を取得するロジック
        // 仮にROLE_USERを返す
        return List.of("ROLE_USER");
    }
}