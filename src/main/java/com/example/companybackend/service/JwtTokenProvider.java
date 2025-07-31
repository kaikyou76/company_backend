//
// import com.example.companybackend.entity.User;
// import io.jsonwebtoken.*;
// import io.jsonwebtoken.security.Keys;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.stereotype.Component;
// 
// import javax.crypto.SecretKey;
// import java.nio.charset.StandardCharsets;
// import java.util.Date;
// import java.util.List;
// import java.util.stream.Collectors;
// 
// /**
//  * JWT トークンプロバイダー（統合版）
//  */
// @Component
// @Slf4j
// public class JwtTokenProvider {
// 
//     private final SecretKey secretKey;
//     private final long tokenValidityInMilliseconds;
//     private final long refreshExpiration;
// 
//     public JwtTokenProvider(
//             @Value("${app.jwt.secret:default-secret-key-for-company-system-at-least-32-chars}") String secret,
//             @Value("${app.jwt.expiration:86400000}") long tokenValidityInMilliseconds,
//             @Value("${app.jwt.refresh-expiration:604800000}") long refreshExpiration) {
//         
//         if (secret.length() < 32) {
//             secret = "default-secret-key-for-company-system-at-least-32-chars";
//         }
//         this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
//         this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
//         this.refreshExpiration = refreshExpiration;
//     }
// 
//     /**
//      * JWT アクセストークン生成（ユーザーオブジェクト直接指定）
//      */
//     public String createToken(User user) {
//         return generateTokenFromUser(user);
//     }
// 
//     /**
//      * JWT アクセストークン生成（認証情報経由）
//      */
//     public String generateToken(Authentication authentication) {
//         Object principal = authentication.getPrincipal();
//         if (principal instanceof User) {
//             return generateTokenFromUser((User) principal);
//         } else {
//             return generateDefaultToken(authentication);
//         }
//     }
// 
//     /**
//      * リフレッシュトークン生成
//      */
//     public String generateRefreshToken(Authentication authentication) {
//         Object principal = authentication.getPrincipal();
//         Date now = new Date();
//         Date expiryDate = new Date(now.getTime() + refreshExpiration);
// 
//         if (principal instanceof User) {
//             User user = (User) principal;
//             return Jwts.builder()
//                     .setSubject(user.getUsername())
//                     .claim("userId", user.getId())
//                     .setIssuedAt(now)
//                     .setExpiration(expiryDate)
//                     .signWith(secretKey, SignatureAlgorithm.HS256)
//                     .compact();
//         } else {
//             return Jwts.builder()
//                     .setSubject(authentication.getName())
//                     .setIssuedAt(now)
//                     .setExpiration(expiryDate)
//                     .signWith(secretKey, SignatureAlgorithm.HS256)
//                     .compact();
//         }
//     }
// 
//     /**
//      * ユーザーオブジェクトからトークン生成（内部メソッド）
//      */
//     private String generateTokenFromUser(User user) {
//         Date now = new Date();
//         Date expiryDate = new Date(now.getTime() + tokenValidityInMilliseconds);
// 
//         return Jwts.builder()
//                 .setSubject(user.getUsername())
//                 .claim("userId", user.getId())
//                 .claim("locationType", user.getLocationType())
//                 .claim("departmentId", user.getDepartmentId())
//                 .claim("positionId", user.getPositionId())
//                 .claim("managerId", user.getManagerId())
//                 .claim("authorities", getAuthoritiesFromUser(user))
//                 .setIssuedAt(now)
//                 .setExpiration(expiryDate)
//                 .signWith(secretKey, SignatureAlgorithm.HS256)
//                 .compact();
//     }
// 
//     /**
//      * デフォルトトークン生成（内部メソッド）
//      */
//     private String generateDefaultToken(Authentication authentication) {
//         Date now = new Date();
//         Date expiryDate = new Date(now.getTime() + tokenValidityInMilliseconds);
// 
//         return Jwts.builder()
//                 .setSubject(authentication.getName())
//                 .claim("authorities", getAuthorities(authentication))
//                 .setIssuedAt(now)
//                 .setExpiration(expiryDate)
//                 .signWith(secretKey, SignatureAlgorithm.HS256)
//                 .compact();
//     }
// 
//     // --- トークン検証・情報取得メソッド ---
// 
//     public String getUsernameFromToken(String token) {
//         try {
//             Claims claims = Jwts.parserBuilder()
//                     .setSigningKey(secretKey)
//                     .build()
//                     .parseClaimsJws(token)
//                     .getBody();
//             return claims.getSubject();
//         } catch (JwtException | IllegalArgumentException e) {
//             log.warn("無効なJWTトークン: {}", e.getMessage());
//             return null;
//         }
//     }
// 
//     public Long getUserIdFromToken(String token) {
//         try {
//             Claims claims = Jwts.parserBuilder()
//                     .setSigningKey(secretKey)
//                     .build()
//                     .parseClaimsJws(token)
//                     .getBody();
//             Long userId = claims.get("userId", Long.class);
//             if (userId != null) {
//                 return userId;
//             }
//             return Long.parseLong(claims.getSubject());
//         } catch (JwtException | IllegalArgumentException e) {
//             log.warn("無効なJWTトークン: {}", e.getMessage());
//             return null;
//         }
//     }
// 
//     public String getLocationTypeFromToken(String token) {
//         try {
//             Claims claims = Jwts.parserBuilder()
//                     .setSigningKey(secretKey)
//                     .build()
//                     .parseClaimsJws(token)
//                     .getBody();
//             return claims.get("locationType", String.class);
//         } catch (JwtException | IllegalArgumentException e) {
//             log.warn("無効なJWTトークン: {}", e.getMessage());
//             return null;
//         }
//     }
// 
//     public Integer getDepartmentIdFromToken(String token) {
//         try {
//             Claims claims = Jwts.parserBuilder()
//                     .setSigningKey(secretKey)
//                     .build()
//                     .parseClaimsJws(token)
//                     .getBody();
//             return claims.get("departmentId", Integer.class);
//         } catch (JwtException | IllegalArgumentException e) {
//             log.warn("無効なJWTトークン: {}", e.getMessage());
//             return null;
//         }
//     }
// 
//     public boolean validateToken(String token) {
//         try {
//             Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
//             return true;
//         } catch (MalformedJwtException e) {
//             log.warn("不正なJWTトークン: {}", e.getMessage());
//         } catch (ExpiredJwtException e) {
//             log.warn("期限切れのJWTトークン: {}", e.getMessage());
//         } catch (UnsupportedJwtException e) {
//             log.warn("サポートされていないJWTトークン: {}", e.getMessage());
//         } catch (IllegalArgumentException e) {
//             log.warn("JWTトークンクレームが空です: {}", e.getMessage());
//         } catch (JwtException e) {
//             log.warn("JWTトークンエラー: {}", e.getMessage());
//         }
//         return false;
//     }
// 
//     public Claims getAllClaimsFromToken(String token) {
//         try {
//             return Jwts.parserBuilder()
//                     .setSigningKey(secretKey)
//                     .build()
//                     .parseClaimsJws(token)
//                     .getBody();
//         } catch (JwtException | IllegalArgumentException e) {
//             log.warn("無効なJWTトークン: {}", e.getMessage());
//             return null;
//         }
//     }
// 
//     public boolean isTokenExpired(String token) {
//         try {
//             Claims claims = getAllClaimsFromToken(token);
//             if (claims == null) return true;
//             return claims.getExpiration().before(new Date());
//         } catch (Exception e) {
//             log.warn("トークン期限チェックエラー: {}", e.getMessage());
//             return true;
//         }
//     }
// 
//     public long getRemainingValidityTime(String token) {
//         try {
//             Claims claims = getAllClaimsFromToken(token);
//             if (claims == null) return 0;
//             long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
//             return Math.max(remaining, 0);
//         } catch (Exception e) {
//             log.warn("トークン残り時間計算エラー: {}", e.getMessage());
//             return 0;
//         }
//     }
// 
//     public Integer getPositionIdFromToken(String token) {
//         try {
//             Claims claims = Jwts.parserBuilder()
//                     .setSigningKey(secretKey)
//                     .build()
//                     .parseClaimsJws(token)
//                     .getBody();
//             return claims.get("positionId", Integer.class);
//         } catch (JwtException | IllegalArgumentException e) {
//             log.warn("無効なJWTトークン: {}", e.getMessage());
//             return null;
//         }
//     }
// 
//     public Integer getManagerIdFromToken(String token) {
//         try {
//             Claims claims = Jwts.parserBuilder()
//                     .setSigningKey(secretKey)
//                     .build()
//                     .parseClaimsJws(token)
//                     .getBody();
//             return claims.get("managerId", Integer.class);
//         } catch (JwtException | IllegalArgumentException e) {
//             log.warn("無効なJWTトークン: {}", e.getMessage());
//             return null;
//         }
//     }
// 
//     /**
//      * 権限情報取得（認証情報経由）
//      */
//     private List<String> getAuthorities(Authentication authentication) {
//         return authentication.getAuthorities().stream()
//                 .map(GrantedAuthority::getAuthority)
//                 .collect(Collectors.toList());
//     }
// 
//     /**
//      * 権限情報取得（ユーザーオブジェクト経用）
//      */
//     private List<String> getAuthoritiesFromUser(User user) {
//         // ユーザーオブジェクトから権限情報を取得するロジック
//         // 例: return user.getRoles().stream()... 
//         // 実際の実装はUserオブジェクトの構造に依存
//         return List.of(); // ダミー実装（実際のプロジェクトで実装必要）
//     }
// }
