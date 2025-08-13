package com.example.companybackend.service;

import org.springframework.security.core.Authentication;

public interface JwtTokenProviderService {
    String generateToken(String username);
    String generateToken(Authentication authentication);
    String generateRefreshToken(Authentication authentication);
    String generateRefreshToken(String username);
    boolean validateToken(String token);
    String getUsername(String token);
}
