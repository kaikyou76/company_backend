package com.example.companybackend.security.test.suite;

import com.example.companybackend.security.test.sql.*;
import com.example.companybackend.security.test.xss.XssProtectionTest;
import com.example.companybackend.security.test.headers.HttpSecurityHeadersTest;
import com.example.companybackend.security.test.jwt.JwtAuthenticationSecurityTest;
import com.example.companybackend.security.test.rate.RateLimitingTest;
import org.junit.jupiter.api.*;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * セキュリティテスト統合スイート
 * 
 * 目的:
 * - 全セキュリティテストの統合実行
 * - テスト実行順序の制御
 * - 統合テストレポート生成の基盤
 * - セキュリティテスト全体の実行効率向上
 * 
 * 実行順序:
 * 1. JWTセキュリティテスト（認証基盤）
 * 2. HTTPセキュリティヘッダーテスト（基盤）
 * 3. XSS保護テスト（入力検証・出力エスケープ）
 * 4. CSRF保護テスト（状態変更保護）
 * 5. レート制限テスト（DoS攻撃対策）
 * 6. SQLインジェクション保護テスト
 * 7. コマンドインジェクション保護テスト
 * 8. パストラバーサル保護テスト
 * 9. ファイルアップロードセキュリティテスト
 * 
 * 要件対応:
 * - フェーズ5の要件6.1を満たす
 * - 全セキュリティテストの統合実行機能実装
 * - テスト実行順序制御実装
 */
@Suite
@SelectClasses({
                // 認証・基盤セキュリティテスト
                JwtAuthenticationSecurityTest.class,
                HttpSecurityHeadersTest.class,

                // Web攻撃保護テスト
                XssProtectionTest.class,
                RateLimitingTest.class,

                // インジェクション攻撃保護テスト
                SqlInjectionProtectionTest.class,
                CommandInjectionProtectionTest.class,
                PathTraversalProtectionTest.class,

                // ファイル操作セキュリティテスト
                FileUploadSecurityTest.class
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("security-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SecurityTestSuite {

        /**
         * テストスイート開始前の初期化
         */
        @BeforeAll
        static void setUpTestSuite() {
                System.out.println("=== セキュリティテスト統合スイート開始 ===");
                System.out.println("実行対象:");
                System.out.println("1. JWTセキュリティテスト");
                System.out.println("2. HTTPセキュリティヘッダーテスト");
                System.out.println("3. XSS保護テスト");
                System.out.println("4. レート制限テスト");
                System.out.println("5. SQLインジェクション保護テスト");
                System.out.println("6. コマンドインジェクション保護テスト");
                System.out.println("7. パストラバーサル保護テスト");
                System.out.println("8. ファイルアップロードセキュリティテスト");
                System.out.println("=====================================");
        }

        /**
         * テストスイート完了後の処理
         */
        @AfterAll
        static void tearDownTestSuite() {
                System.out.println("=== セキュリティテスト統合スイート完了 ===");
                System.out.println("全てのセキュリティテストが完了しました。");
                System.out.println("詳細な結果はテストレポートを確認してください。");
                System.out.println("=====================================");
        }
}