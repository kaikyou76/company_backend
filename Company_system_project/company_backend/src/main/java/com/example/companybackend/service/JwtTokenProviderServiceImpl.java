package com.example.companybackend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtTokenProviderServiceImpl implements JwtTokenProviderService {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProviderServiceImpl.class);
    
    private final SecretKey secretKey;
    private final long validityMs;
    private final JwtParser jwtParser;

    public JwtTokenProviderServiceImpl(
        @Value("${app.jwt.secret:default-secret-key-for-company-system-at-least-32-chars}") String secret,
        @Value("${app.jwt.expiration:86400000}") long validityMs) {
        
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.validityMs = validityMs;
        // 使用正确的JwtParser构建方式，确保调用build()方法
        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
    }

    @Override
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);
        
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            // 使用预构建的jwtParser实例，避免直接调用Jwts.parserBuilder()
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException | UnsupportedJwtException | 
                MalformedJwtException | SignatureException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String getUsername(String token) {
        // 使用预构建的jwtParser实例，避免直接调用Jwts.parserBuilder()
        return jwtParser.parseSignedClaims(token).getPayload().getSubject();
    }
}