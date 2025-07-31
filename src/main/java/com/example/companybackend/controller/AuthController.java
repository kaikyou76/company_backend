package com.example.companybackend.controller;

import com.example.companybackend.entity.User;
import com.example.companybackend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * ユーザーログイン
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            if (username == null || password == null || username.isBlank() || password.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("ユーザー名とパスワードは必須です");
            }

            Map<String, String> tokens = authService.authenticateUser(username, password);
            return ResponseEntity.ok(tokens);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("認証処理中にエラーが発生しました");
        }
    }

    /**
     * ユーザー登録
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            // 必須フィールドの検証
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("ユーザー名は必須です");
            }
            
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("パスワードは必須です");
            }
            
            if (user.getLocationType() == null || user.getLocationType().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("勤務場所タイプは必須です");
            }
            
            // location_typeのバリデーション (officeまたはclient)
            if (!"office".equals(user.getLocationType()) && !"client".equals(user.getLocationType())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("勤務場所タイプは 'office' または 'client' である必要があります");
            }
            
            // clientの場合、緯度経度はオプションだが、両方設定するか両方nullにする
            if ("client".equals(user.getLocationType())) {
                if ((user.getClientLatitude() == null) != (user.getClientLongitude() == null)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("クライアント勤務地の緯度と経度は両方設定するか、両方nullにする必要があります");
                }
            } else {
                // officeの場合は緯度経度をnullに設定
                user.setClientLatitude(null);
                user.setClientLongitude(null);
            }
            
            User registeredUser = authService.registerUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("ユーザー登録中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * CSVファイルからのユーザー一括登録
     */
    @PostMapping("/csvregister")
    public ResponseEntity<?> registerUsersFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("ファイルが空です");
            }
            
            if (!"text/csv".equals(file.getContentType()) && 
                !"application/vnd.ms-excel".equals(file.getContentType())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("CSVファイルのみアップロード可能です");
            }
            
            String result = authService.registerUsersFromCsv(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("CSV処理中に予期せぬエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * 管理者によるユーザー登録
     */
    @PostMapping("/admin/register")
    public ResponseEntity<?> registerUserByAdmin(@RequestBody User user) {
        try {
            // 必須フィールドの検証
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("ユーザー名は必須です");
            }
            
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("パスワードは必須です");
            }
            
            if (user.getLocationType() == null || user.getLocationType().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("勤務場所タイプは必須です");
            }
            
            // location_typeのバリデーション
            if (!"office".equals(user.getLocationType()) && !"client".equals(user.getLocationType())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("勤務場所タイプは 'office' または 'client' である必要があります");
            }
            
            // 管理者登録では部署、役職、マネージャーの設定が可能
            User registeredUser = authService.registerUserByAdmin(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("管理者ユーザー登録中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * ユーザー名重複チェック
     */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsernameExists(@RequestParam String username) {
        try {
            if (username == null || username.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("ユーザー名を指定してください");
            }
            
            boolean exists = authService.checkUsernameExists(username);
            return ResponseEntity.ok(exists);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("チェック処理中にエラーが発生しました");
        }
    }
}