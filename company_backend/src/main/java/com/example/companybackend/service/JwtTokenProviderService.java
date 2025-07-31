package com.example.companybackend.service;

public interface JwtTokenProviderService {
    String generateToken(String username);
    boolean validateToken(String token);
    String getUsername(String token);
}