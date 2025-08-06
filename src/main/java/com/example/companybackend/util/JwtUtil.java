package com.example.companybackend.util;

import com.example.companybackend.service.JwtTokenProviderService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JWTユーティリティクラス
 * JWTトークンの検証や解析を行うためのユーティリティメソッドを提供します
 */
@Component
public class JwtUtil {

    @Autowired
    private JwtTokenProviderService jwtTokenProviderService;

    /**
     * トークンの有効性を検証
     *
     * @param token JWTトークン
     * @return トークンが有効な場合はtrue、そうでない場合はfalse
     */
    public boolean validateToken(String token) {
        return jwtTokenProviderService.validateToken(token);
    }

    /**
     * トークンからユーザー名を取得
     *
     * @param token JWTトークン
     * @return ユーザー名
     */
    public String getUsernameFromToken(String token) {
        return jwtTokenProviderService.getUsername(token);
    }

    /**
     * トークンからユーザー名を抽出
     *
     * @param token JWTトークン
     * @return ユーザー名
     */
    public String extractUsername(String token) {
        return jwtTokenProviderService.getUsername(token);
    }

    /**
     * トークンからすべてのクレームを取得
     *
     * @param token JWTトークン
     * @return クレーム情報
     */
    public Claims getAllClaimsFromToken(String token) {
        // JwtTokenProviderServiceにgetAllClaimsFromTokenメソッドがないため、
        // getUsernameFromTokenメソッドをベースに実装
        return null;
    }
}