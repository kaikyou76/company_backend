package com.example.companybackend.controller;

import com.example.companybackend.dto.auth.*;
import com.example.companybackend.entity.User;
import com.example.companybackend.service.AuthService;
import com.example.companybackend.util.CsvParsingUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * ユーザー登録（一般）
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("ユーザー登録リクエスト受信: username={}", registerRequest.getUsername());

        try {
            // パスワードと確認用パスワードの一致チェック
            if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
                logger.warn("パスワード不一致: username={}", registerRequest.getUsername());
                
                RegisterResponse errorResponse = RegisterResponse.builder()
                        .success(false)
                        .message("パスワードと確認用パスワードが一致しません")
                        .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Userエンティティに変換
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setPasswordHash(registerRequest.getPassword());
            user.setFullName(registerRequest.getFullName());
            user.setLocationType(registerRequest.getLocationType());
            user.setClientLatitude(registerRequest.getClientLatitude());
            user.setClientLongitude(registerRequest.getClientLongitude());
            user.setDepartmentId(registerRequest.getDepartmentId());
            user.setPositionId(registerRequest.getPositionId());
            user.setManagerId(registerRequest.getManagerId());

            User registeredUser = authService.registerUser(user);
            logger.info("ユーザー登録成功: username={}", registerRequest.getUsername());
            
            RegisterResponse response = RegisterResponse.success(registeredUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            logger.error("ユーザー登録エラー: username={}, error={}", registerRequest.getUsername(), e.getMessage());
            
            RegisterResponse errorResponse = RegisterResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("ユーザー登録エラー: username={}, error={}", registerRequest.getUsername(), e.getMessage());
            
            RegisterResponse errorResponse = RegisterResponse.builder()
                    .success(false)
                    .message("ユーザー登録中にエラーが発生しました")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * ログイン
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("ログインリクエスト受信: employeeCode={}", loginRequest.getEmployeeCode());

        try {
            String accessToken = authService.authenticateUser(loginRequest.getEmployeeCode(), loginRequest.getPassword());
            logger.info("ログイン成功: employeeCode={}", loginRequest.getEmployeeCode());
            
            // ユーザー情報を取得
            User user = authService.getUserByUsername(loginRequest.getEmployeeCode());
            String departmentName = authService.getDepartmentNameById(user.getDepartmentId());
            String positionName = authService.getPositionNameById(user.getPositionId());
            
            LoginResponse response = LoginResponse.success(accessToken, "", user, departmentName, positionName);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("ログインエラー: employeeCode={}, error={}", loginRequest.getEmployeeCode(), e.getMessage());
            
            LoginResponse errorResponse = LoginResponse.error("認証情報が正しくありません");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            logger.error("ログインエラー: employeeCode={}, error={}", loginRequest.getEmployeeCode(), e.getMessage());
            
            LoginResponse errorResponse = LoginResponse.error("ログイン処理中にエラーが発生しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * トークンリフレッシュ
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        logger.info("トークンリフレッシュリクエスト受信");

        try {
            // TODO: トークンリフレッシュ処理の実装
            LoginResponse errorResponse = LoginResponse.error("リフレッシュトークンが無効です");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("トークンリフレッシュエラー: error={}", e.getMessage());
            
            LoginResponse errorResponse = LoginResponse.error("トークンリフレッシュ処理中にエラーが発生しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }    /**
     * ログアウト
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestHeader("Authorization") String authorizationHeader) {
        logger.info("ログアウトリクエスト受信");
        
        try {
            // AuthorizationヘッダーからBearerトークンを抽出
            String token = null;
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
            }

            // TODO: 実際のプロジェクトでは、トークンをブラックリストに追加するなどの
            // トークン無効化処理を実装する必要があります
            // 例: Redisにトークンを保存し、有効期限まで保持するなど
            
            LogoutResponse response = LogoutResponse.success();
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("ログアウトエラー: error={}", e.getMessage());
            
            LogoutResponse errorResponse = LogoutResponse.error("ログアウト処理中にエラーが発生しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * ユーザー名重複チェック
     */
    @GetMapping("/check-username")
    public ResponseEntity<UsernameCheckResponse> checkUsernameAvailability(@RequestParam String username) {
        try {
            if (username == null || username.isBlank()) {
                UsernameCheckResponse errorResponse = UsernameCheckResponse.of(false, "ユーザー名を指定してください");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            boolean exists = authService.checkUsernameExists(username);
            boolean available = !exists;

            UsernameCheckResponse response = UsernameCheckResponse.of(available, 
                    available ? "ユーザー名は利用可能です" : "ユーザー名は既に使用されています");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("ユーザー名チェックエラー: username={}, error={}", username, e.getMessage());
            
            UsernameCheckResponse errorResponse = UsernameCheckResponse.of(false, "チェック処理中にエラーが発生しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 管理者によるユーザー登録
     */
    @PostMapping("/admin-register")
    public ResponseEntity<AdminRegisterResponse> registerAdminUser(
            @Valid @RequestBody AdminRegisterRequest adminRegisterRequest,
            @RequestHeader("X-Admin-Username") String adminUsername) {

        logger.info("管理者ユーザー登録リクエスト受信: username={}, positionId={}, createdBy={}",
                   adminRegisterRequest.getUsername(), adminRegisterRequest.getPositionId(), adminUsername);

        try {
            // パスワードと確認用パスワードの一致チェック
            if (!adminRegisterRequest.getPassword().equals(adminRegisterRequest.getConfirmPassword())) {
                logger.warn("パスワード不一致: username={}", adminRegisterRequest.getUsername());
                
                AdminRegisterResponse errorResponse = new AdminRegisterResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("パスワードと確認用パスワードが一致しません");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Userエンティティに変換
            User user = new User();
            user.setUsername(adminRegisterRequest.getUsername());
            user.setPasswordHash(adminRegisterRequest.getPassword());
            user.setFullName(adminRegisterRequest.getFullName());
            user.setLocationType(adminRegisterRequest.getLocationType() != null ? 
                                adminRegisterRequest.getLocationType() : "office");
            user.setClientLatitude(adminRegisterRequest.getClientLatitude());
            user.setClientLongitude(adminRegisterRequest.getClientLongitude());
            user.setDepartmentId(adminRegisterRequest.getDepartmentId());
            user.setPositionId(adminRegisterRequest.getPositionId());
            user.setManagerId(adminRegisterRequest.getManagerId());

            // 役職IDの検証
            if (adminRegisterRequest.getPositionId() == null || adminRegisterRequest.getPositionId() <= 0) {
                logger.warn("無効な役職ID: positionId={}", adminRegisterRequest.getPositionId());
                
                AdminRegisterResponse errorResponse = new AdminRegisterResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("役職IDは有効な正の数値を指定してください");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            User registeredUser = authService.registerUserByAdmin(user, adminUsername);
            logger.info("管理者ユーザー登録成功: username={}, createdBy={}",
                       adminRegisterRequest.getUsername(), adminUsername);
            
            // 部署名と役職名を取得
            String departmentName = authService.getDepartmentNameById(user.getDepartmentId());
            String positionName = authService.getPositionNameById(user.getPositionId());
            
            AdminRegisterResponse response = AdminRegisterResponse.success(registeredUser, positionName, departmentName, adminUsername);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            logger.error("管理者ユーザー登録エラー: username={}, createdBy={}, error={}",
                        adminRegisterRequest.getUsername(), adminUsername, e.getMessage());
            
            // 例外に応じた適切なステータスコードを返す
            if (e.getMessage().contains("権限が不足しています")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AdminRegisterResponse.error(e.getMessage()));
            } else if (e.getMessage().contains("既に使用されています") || 
                      e.getMessage().contains("無効な") ||
                      e.getMessage().contains("一致しません")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AdminRegisterResponse.error(e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AdminRegisterResponse.error(e.getMessage()));
            }
        } catch (Exception e) {
            logger.error("管理者ユーザー登録エラー: username={}, createdBy={}, error={}",
                        adminRegisterRequest.getUsername(), adminUsername, e.getMessage());
            
            AdminRegisterResponse errorResponse = new AdminRegisterResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("管理者ユーザー登録中にエラーが発生しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 管理者役職一覧取得
     */
    @GetMapping("/admin-positions")
    public ResponseEntity<AdminPositionsResponse> getAdminPositions() {
        try {
            // 管理者レベル（level >= 5）の役職一覧を返す
            List<AdminPositionsResponse.PositionData> positions = authService.getAdminPositions();
            
            AdminPositionsResponse response = AdminPositionsResponse.of(positions);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("管理者役職一覧取得エラー: {}", e.getMessage());
            
            AdminPositionsResponse errorResponse = AdminPositionsResponse.error("管理者役職一覧取得中にエラーが発生しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * CSVファイルからのユーザー一括登録
     */
    @PostMapping(value = "/csvregister", consumes = "multipart/form-data")
    public ResponseEntity<CsvRegisterResponse> registerUsersFromCsv(
            @RequestParam("file") MultipartFile file) {

        logger.info("CSV一括登録リクエスト受信: filename={}, size={}",
                   file.getOriginalFilename(), file.getSize());

        try {
            // CSV ファイル解析
            List<CsvUserData> csvUsers = CsvParsingUtil.parseCsvFile(file);
            logger.info("CSV解析完了: totalRows={}", csvUsers.size());

            // 一括登録処理
            int[] result = authService.registerUsersFromCsv(csvUsers);
            
            logger.info("CSV一括登録完了: successCount={}, errorCount={}", result[0], result[1]);
            
            CsvRegisterResponse response = CsvRegisterResponse.success(result[0], result[1]);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            logger.error("CSV一括登録エラー: filename={}, error={}",
                        file.getOriginalFilename(), e.getMessage());
            
            CsvRegisterResponse errorResponse = CsvRegisterResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("CSV一括登録エラー: filename={}, error={}",
                        file.getOriginalFilename(), e.getMessage());
            
            CsvRegisterResponse errorResponse = CsvRegisterResponse.error("CSV一括登録処理中にエラーが発生しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * CSVテンプレート取得
     */
    @GetMapping("/csv-template")
    public ResponseEntity<CsvTemplateResponse> getCsvTemplate() {
        try {
            String[] headers = {"username", "password", "fullname", "location_type", "client_latitude", 
                               "client_longitude", "department_id", "position_id", "manager_id"};
            
            String[] sampleData = {"user@example.com", "password123", "山田太郎", "office", "", "", "1", "3", "1"};

            CsvTemplateResponse response = CsvTemplateResponse.of(headers, sampleData);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("CSV テンプレート取得エラー: {}", e.getMessage());
            
            CsvTemplateResponse errorResponse = CsvTemplateResponse.error("CSVテンプレート取得中にエラーが発生しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}