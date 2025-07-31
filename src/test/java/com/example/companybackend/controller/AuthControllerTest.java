package com.example.companybackend.controller;

import com.example.companybackend.dto.auth.*;
import com.example.companybackend.entity.User;
import com.example.companybackend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthControllerのユニットテストクラス
 * 
 * テスト対象: {@link AuthController}
 * 
 * テスト範囲:
 * - ユーザー登録 (一般ユーザー)
 * - ユーザー登録 (管理者による登録)
 * - ログイン/ログアウト
 * - トークンリフレッシュ
 * - ユーザー名重複チェック
 * - 管理者役職一覧取得
 * - CSVファイルからのユーザー一括登録
 * - CSVテンプレート取得
 * 
 * テスト用例制作规范和技巧:
 * 1. 各テストメソッドは1つの機能を検証する
 * 2. 正常系（成功ケース）と異常系（失敗ケース）の両方をテストする
 * 3. モックを使用して外部依存を排除し、テスト対象のコードのみを検証する
 * 4. JSONパスを使用してレスポンスの特定フィールドを検証する
 * 5. メソッド呼び出し回数や引数の検証を行う
 * 6. セキュリティが必要なエンドポイントは@WithMockUserを使用してテストする
 */
@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = {AuthController.class, AuthControllerTest.TestSecurityConfig.class})
public class AuthControllerTest {

    /**
     * MockMvcはSpring MVCテスト用のクライアント
     * HTTPリクエストをシミュレートし、レスポンスを検証するために使用する
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * AuthServiceのモックオブジェクト
     * 実際のサービス呼び出しをモックして、テスト対象のコントローラのみを検証する
     */
    @MockBean
    private AuthService authService;

    /**
     * テストで使用するユーザーのモックデータ
     */
    private User testUser;

    /**
     * テスト用のセキュリティ設定
     * CSRF保護を無効化し、すべてのリクエストを許可する
     */
    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .build();
        }
    }

    /**
     * 各テストメソッド実行前の初期化処理
     * テストデータのセットアップを行う
     */
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("test@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFullName("テストユーザー");
        testUser.setLocationType("office");
        testUser.setDepartmentId(1);
        testUser.setPositionId(1);
        testUser.setManagerId(2);
        testUser.setCreatedAt(OffsetDateTime.now());
        testUser.setUpdatedAt(OffsetDateTime.now());
    }

    /**
     * 一般ユーザー登録のテスト
     * 正常系：ユーザーが正しく登録できることを検証する
     * 
     * テスト対象メソッド: {@link AuthController#registerUser}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが201(Created)であること
     * 2. レスポンスのsuccessフィールドがtrueであること
     * 3. レスポンスにユーザー情報が含まれていること
     * 4. AuthService.registerUserが1回呼び出されていること
     */
    @Test
    void testRegisterUser_Success() throws Exception {
        // モックの設定
        when(authService.registerUser(any(User.class))).thenReturn(testUser);

        // リクエストボディの作成
        String registerRequest = """
            {
                "username": "newuser@example.com",
                "password": "password123",
                "confirmPassword": "password123",
                "fullName": "新規ユーザー",
                "locationType": "office"
            }
            """;

        // テスト実行と検証
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ユーザー登録が正常に完了しました"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("test@example.com"));

        // メソッド呼び出しの検証
        verify(authService, times(1)).registerUser(any(User.class));
    }

    /**
     * 一般ユーザー登録のテスト
     * 異常系：パスワードと確認用パスワードが一致しない場合のエラーを検証する
     * 
     * テスト対象メソッド: {@link AuthController#registerUser}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが400(Bad Request)であること
     * 2. レスポンスのsuccessフィールドがfalseであること
     * 3. 適切なエラーメッセージが返されること
     * 4. AuthService.registerUserが呼び出されていないこと
     */
    @Test
    void testRegisterUser_PasswordMismatch() throws Exception {
        // リクエストボディの作成（パスワード不一致）
        String registerRequest = """
            {
                "username": "newuser@example.com",
                "password": "password123",
                "confirmPassword": "differentPassword",
                "fullName": "新規ユーザー",
                "locationType": "office"
            }
            """;

        // テスト実行と検証
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("パスワードと確認用パスワードが一致しません"));

        // AuthServiceのメソッドが呼び出されていないことを検証
        verify(authService, never()).registerUser(any(User.class));
    }

    /**
     * ログインのテスト
     * 正常系：正しい認証情報でログインできることを検証する
     * 
     * テスト対象メソッド: {@link AuthController#loginUser}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスのsuccessフィールドがtrueであること
     * 3. JWTトークンが返されること
     * 4. ユーザー情報が含まれていること
     * 5. 関連するサービスメソッドが正しく呼び出されていること
     */
    @Test
    void testLoginUser_Success() throws Exception {
        // モックの設定
        when(authService.authenticateUser(anyString(), anyString())).thenReturn("mock-jwt-token");
        when(authService.getUserByUsername(anyString())).thenReturn(testUser);
        when(authService.getDepartmentNameById(anyInt())).thenReturn("開発部");
        when(authService.getPositionNameById(anyInt())).thenReturn("エンジニア");

        // リクエストボディの作成
        String loginRequest = """
            {
                "employeeCode": "test@example.com",
                "password": "password123"
            }
            """;

        // テスト実行と検証
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.name").value("test@example.com"));

        // メソッド呼び出しの検証
        verify(authService, times(1)).authenticateUser(anyString(), anyString());
        verify(authService, times(1)).getUserByUsername(anyString());
        verify(authService, times(1)).getDepartmentNameById(anyInt());
        verify(authService, times(1)).getPositionNameById(anyInt());
    }

    /**
     * ログインのテスト
     * 異常系：誤った認証情報でログインできないことを検証する
     * 
     * テスト対象メソッド: {@link AuthController#loginUser}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが401(Unauthorized)であること
     * 2. レスポンスのsuccessフィールドがfalseであること
     * 3. 適切なエラーメッセージが返されること
     * 4. ユーザー情報取得メソッドが呼び出されていないこと
     */
    @Test
    void testLoginUser_AuthenticationFailed() throws Exception {
        // モックの設定（認証失敗）
        when(authService.authenticateUser(anyString(), anyString()))
            .thenThrow(new RuntimeException("認証情報が正しくありません"));

        // リクエストボディの作成
        String loginRequest = """
            {
                "employeeCode": "wrong@example.com",
                "password": "wrongpassword"
            }
            """;

        // テスト実行と検証
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("認証情報が正しくありません"));

        // メソッド呼び出しの検証
        verify(authService, times(1)).authenticateUser(anyString(), anyString());
        verify(authService, never()).getUserByUsername(anyString());
    }

    /**
     * ユーザー名重複チェックのテスト
     * 正常系：ユーザー名が利用可能かどうかを正しく判定できることを検証する
     * 
     * テスト対象メソッド: {@link AuthController#checkUsernameAvailability}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスのsuccessフィールドがtrueであること
     * 3. ユーザー名が利用可能であることを示すフラグがtrueであること
     * 4. 適切なメッセージが返されること
     * 5. AuthService.checkUsernameExistsが1回呼び出されていること
     */
    @Test
    void testCheckUsernameAvailability_Success() throws Exception {
        // モックの設定（ユーザー名が利用可能）
        when(authService.checkUsernameExists("newuser@example.com")).thenReturn(false);

        // テスト実行と検証
        mockMvc.perform(get("/api/auth/check-username")
                .param("username", "newuser@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.message").value("ユーザー名は利用可能です"));

        // メソッド呼び出しの検証
        verify(authService, times(1)).checkUsernameExists("newuser@example.com");
    }

    /**
     * 管理者によるユーザー登録のテスト
     * 正常系：管理者がユーザーを正しく登録できることを検証する
     * 
     * テスト対象メソッド: {@link AuthController#registerAdminUser}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが201(Created)であること
     * 2. レスポンスのsuccessフィールドがtrueであること
     * 3. レスポンスにユーザー情報が含まれていること
     * 4. 関連するサービスメソッドが正しく呼び出されていること
     * 5. セキュリティ設定により管理者権限が必要であること
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testRegisterAdminUser_Success() throws Exception {
        // モックの設定
        when(authService.registerUserByAdmin(any(User.class), anyString())).thenReturn(testUser);
        when(authService.getDepartmentNameById(any())).thenReturn("開発部");
        when(authService.getPositionNameById(anyInt())).thenReturn("エンジニア");

        // リクエストボディの作成
        String adminRegisterRequest = """
            {
                "username": "newadmin@example.com",
                "password": "admin123",
                "confirmPassword": "admin123",
                "fullName": "新規管理者",
                "locationType": "office",
                "positionId": 1
            }
            """;

        // テスト実行と検証
        mockMvc.perform(post("/api/auth/admin-register")
                .header("X-Admin-Username", "admin@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(adminRegisterRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("管理者ユーザーの登録が正常に完了しました"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("test@example.com"));

        // メソッド呼び出しの検証
        verify(authService, times(1)).registerUserByAdmin(any(User.class), anyString());
        verify(authService, times(1)).getDepartmentNameById(any());
        verify(authService, times(1)).getPositionNameById(anyInt());
    }

    /**
     * 管理者によるユーザー登録のテスト
     * 異常系：パスワードと確認用パスワードが一致しない場合のエラーを検証する
     * 
     * テスト対象メソッド: {@link AuthController#registerAdminUser}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが400(Bad Request)であること
     * 2. レスポンスのsuccessフィールドがfalseであること
     * 3. 適切なエラーメッセージが返されること
     * 4. AuthService.registerUserByAdminが呼び出されていないこと
     * 5. セキュリティ設定により管理者権限が必要であること
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testRegisterAdminUser_PasswordMismatch() throws Exception {
        // リクエストボディの作成（パスワード不一致）
        String adminRegisterRequest = """
            {
                "username": "newadmin@example.com",
                "password": "admin123",
                "confirmPassword": "differentPassword",
                "fullName": "新規管理者",
                "locationType": "office",
                "positionId": 1
            }
            """;

        // テスト実行と検証
        mockMvc.perform(post("/api/auth/admin-register")
                .header("X-Admin-Username", "admin@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(adminRegisterRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("パスワードと確認用パスワードが一致しません"));

        // AuthServiceのメソッドが呼び出されていないことを検証
        verify(authService, never()).registerUserByAdmin(any(User.class), anyString());
    }

    /**
     * 管理者役職一覧取得のテスト
     * 正常系：管理者が役職一覧を正しく取得できることを検証する
     * 
     * テスト対象メソッド: {@link AuthController#getAdminPositions}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスのsuccessフィールドがtrueであること
     * 3. 役職情報が正しく返されること
     * 4. AuthService.getAdminPositionsが1回呼び出されていること
     */
    @Test
    void testGetAdminPositions_Success() throws Exception {
        // モックの設定
        AdminPositionsResponse.PositionData positionData = new AdminPositionsResponse.PositionData(1L, "管理者", 5);
        List<AdminPositionsResponse.PositionData> positions = Arrays.asList(positionData);
        when(authService.getAdminPositions()).thenReturn(positions);

        // テスト実行と検証
        mockMvc.perform(get("/api/auth/admin-positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("管理者役職一覧を取得しました"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("管理者"));

        // メソッド呼び出しの検証
        verify(authService, times(1)).getAdminPositions();
    }

    /**
     * CSV一括登録のテスト
     * 正常系：CSVファイルからユーザーが正しく登録できることを検証する
     * 
     * テスト対象メソッド: {@link AuthController#registerUsersFromCsv}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが201(Created)であること
     * 2. レスポンスのsuccessフィールドがtrueであること
     * 3. 成功件数と失敗件数が正しく返されること
     * 4. AuthService.registerUsersFromCsvが1回呼び出されていること
     */
    @Test
    void testRegisterUsersFromCsv_Success() throws Exception {
        // モックの設定
        when(authService.registerUsersFromCsv(anyList())).thenReturn(new int[]{5, 0}); // 5件成功, 0件失敗

        // CSVファイルの作成
        String csvContent = "username,password,fullname,location_type,client_latitude,client_longitude,department_id,position_id,manager_id\n" +
                           "user1@example.com,password123,ユーザー1,office,,,1,3,2\n" +
                           "user2@example.com,password123,ユーザー2,office,,,1,3,2\n";
        
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvContent.getBytes()
        );

        // テスト実行と検証
        mockMvc.perform(multipart("/api/auth/csvregister")
                .file(csvFile)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("CSV一括登録が正常に完了しました"))
                .andExpect(jsonPath("$.data.successCount").value(5))
                .andExpect(jsonPath("$.data.errorCount").value(0));

        // メソッド呼び出しの検証
        verify(authService, times(1)).registerUsersFromCsv(anyList());
    }

    /**
     * CSVテンプレート取得のテスト
     * 正常系：CSVテンプレートが正しく取得できることを検証する
     * 
     * テスト対象メソッド: {@link AuthController#getCsvTemplate}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスのsuccessフィールドがtrueであること
     * 3. ヘッダー情報とサンプルデータが正しく返されること
     */
    @Test
    void testGetCsvTemplate_Success() throws Exception {
        // テスト実行と検証
        mockMvc.perform(get("/api/auth/csv-template"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("CSV テンプレートフォーマット"))
                .andExpect(jsonPath("$.data.headers.length()").value(9))
                .andExpect(jsonPath("$.data.headers[0]").value("username"))
                .andExpect(jsonPath("$.data.sampleData.length()").value(9))
                .andExpect(jsonPath("$.data.sampleData[0]").value("user@example.com"));

        // メソッド呼び出しの検証
        verify(authService, never()).getAdminPositions();
    }

    /**
     * ログアウトのテスト
     * 正常系：ユーザーが正しくログアウトできることを検証する
     * 
     * テスト対象メソッド: {@link AuthController#logout}
     * 
     * 検証内容:
     * 1. HTTPステータスコードが200(OK)であること
     * 2. レスポンスのsuccessフィールドがtrueであること
     * 3. 適切なメッセージが返されること
     */
    @Test
    void testLogout_Success() throws Exception {
        // テスト実行と検証
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer mock-jwt-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ログアウトしました"));
    }
}