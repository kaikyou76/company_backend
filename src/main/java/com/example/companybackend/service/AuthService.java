package com.example.companybackend.service;

import com.example.companybackend.entity.User;
import com.example.companybackend.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProviderService tokenProvider;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProviderService tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * ユーザー認証とトークン生成
     */
    public Map<String, String> authenticateUser(String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            String jwt = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);
            
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", jwt);
            tokens.put("refreshToken", refreshToken);
            
            return tokens;
        } catch (Exception e) {
            throw new RuntimeException("認証に失敗しました: " + e.getMessage());
        }
    }

    /**
     * ユーザー登録（一般）
     */
    public User registerUser(User user) {
        // ユーザー名重複チェック
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("ユーザー名は既に使用されています");
        }

        // パスワードハッシュ化
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        
        // 作成日時と更新日時を設定
        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        
        // manager_id, department_id, position_idはデフォルトでnull（後で管理者が設定可能）
        
        return userRepository.save(user);
    }

    /**
     * 管理者によるユーザー登録
     */
    public User registerUserByAdmin(User user) {
        // ユーザー名重複チェック
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("ユーザー名は既に使用されています");
        }

        // パスワードハッシュ化
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        
        // 作成日時と更新日時を設定
        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        
        // 管理者による登録では部署、役職、マネージャーの設定を許可
        // これらの値は既にuserオブジェクトに設定されているとみなす
        
        return userRepository.save(user);
    }

    /**
     * IDによるユーザー取得
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * ユーザー名存在チェック
     */
    public boolean checkUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * CSVファイルからユーザー一括登録
     * CSVフォーマット: username,password,location_type,client_latitude,client_longitude,department_id,position_id,manager_id
     */
    public String registerUsersFromCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<String> lines = reader.lines().collect(Collectors.toList());
            
            if (lines.isEmpty()) {
                throw new RuntimeException("CSVファイルが空です");
            }
            
            // ヘッダー行をチェック
            String header = lines.get(0);
            if (!isValidCsvHeader(header)) {
                throw new RuntimeException("CSVヘッダーが正しくありません。期待される形式: username,password,location_type,client_latitude,client_longitude,department_id,position_id,manager_id");
            }
            
            // ユーザーデータを解析
            List<User> users = lines.stream()
                    .skip(1) // ヘッダー行をスキップ
                    .filter(line -> !line.trim().isEmpty()) // 空行をスキップ
                    .map(this::parseUserFromCsvLine)
                    .collect(Collectors.toList());

            if (users.isEmpty()) {
                throw new RuntimeException("有効なユーザーデータが見つかりません");
            }

            // ユーザー名重複チェック（DB内での重複）
            List<String> existingUsernames = users.stream()
                    .filter(user -> userRepository.existsByUsername(user.getUsername()))
                    .map(User::getUsername)
                    .collect(Collectors.toList());
            
            if (!existingUsernames.isEmpty()) {
                throw new RuntimeException("データベース内に重複ユーザー名が存在します: " + String.join(", ", existingUsernames));
            }
            
            // CSV内での重複チェック
            List<String> csvUsernames = users.stream().map(User::getUsername).collect(Collectors.toList());
            List<String> duplicatesInCsv = csvUsernames.stream()
                    .filter(name -> csvUsernames.indexOf(name) != csvUsernames.lastIndexOf(name))
                    .distinct()
                    .collect(Collectors.toList());
                    
            if (!duplicatesInCsv.isEmpty()) {
                throw new RuntimeException("CSV内に重複ユーザー名が存在します: " + String.join(", ", duplicatesInCsv));
            }

            // パスワードハッシュ化してバッチ保存
            OffsetDateTime now = OffsetDateTime.now();
            users.forEach(user -> {
                user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
            });
            
            userRepository.saveAll(users);

            return users.size() + "人のユーザーを登録しました";
        } catch (RuntimeException e) {
            throw e; // カスタム例外は再スロー
        } catch (Exception e) {
            throw new RuntimeException("CSVファイル処理エラー: " + e.getMessage(), e);
        }
    }
    
    /**
     * CSVヘッダーの妥当性チェック
     */
    private boolean isValidCsvHeader(String header) {
        String expectedHeader = "username,password,location_type,client_latitude,client_longitude,department_id,position_id,manager_id";
        return expectedHeader.equalsIgnoreCase(header.trim());
    }

    /**
     * CSV行からユーザーオブジェクトへの変換
     * フォーマット: username,password,location_type,client_latitude,client_longitude,department_id,position_id,manager_id
     */
    private User parseUserFromCsvLine(String line) {
        String[] values = line.split(",", -1); // -1を使用して空文字も含める
        if (values.length < 3) {
            throw new RuntimeException("無効なCSV形式: " + line);
        }

        User user = new User();
        
        // 必須項目
        user.setUsername(values[0].trim());
        user.setPasswordHash(values[1].trim());
        user.setLocationType(values[2].trim());
        
        // バリデーション
        if (user.getUsername().isEmpty()) {
            throw new RuntimeException("ユーザー名が空です: " + line);
        }
        if (user.getPasswordHash().isEmpty()) {
            throw new RuntimeException("パスワードが空です: " + line);
        }
        if (!"office".equals(user.getLocationType()) && !"client".equals(user.getLocationType())) {
            throw new RuntimeException("勤務場所タイプは 'office' または 'client' である必要があります: " + line);
        }
        
        // オプション項目の処理
        if (values.length > 3 && !values[3].trim().isEmpty()) {
            user.setClientLatitude(parseDoubleSafely(values[3].trim(), line));
        }
        
        if (values.length > 4 && !values[4].trim().isEmpty()) {
            user.setClientLongitude(parseDoubleSafely(values[4].trim(), line));
        }
        
        if (values.length > 5 && !values[5].trim().isEmpty()) {
            user.setDepartmentId(parseIntegerSafely(values[5].trim(), line));
        }
        
        if (values.length > 6 && !values[6].trim().isEmpty()) {
            user.setPositionId(parseIntegerSafely(values[6].trim(), line));
        }
        
        if (values.length > 7 && !values[7].trim().isEmpty()) {
            user.setManagerId(parseIntegerSafely(values[7].trim(), line));
        }
        
        // クライアント勤務の場合の緯度経度整合性チェック
        if ("client".equals(user.getLocationType())) {
            if ((user.getClientLatitude() == null) != (user.getClientLongitude() == null)) {
                throw new RuntimeException("クライアント勤務地の緯度と経度は両方設定するか、両方nullにする必要があります: " + line);
            }
        } else {
            // オフィス勤務の場合は緯度経度をnullに設定
            user.setClientLatitude(null);
            user.setClientLongitude(null);
        }
        
        return user;
    }

    /**
     * 安全なdouble変換
     */
    private Double parseDoubleSafely(String value, String line) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("数値変換エラー (double): " + value + " in line: " + line);
        }
    }
    
    /**
     * 安全なInteger変換
     */
    private Integer parseIntegerSafely(String value, String line) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("数値変換エラー (integer): " + value + " in line: " + line);
        }
    }
}