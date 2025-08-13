package com.example.companybackend.service;

import com.example.companybackend.dto.auth.AdminPositionsResponse;
import com.example.companybackend.dto.auth.CsvUserData;
import com.example.companybackend.entity.Department;
import com.example.companybackend.entity.Position;
import com.example.companybackend.entity.User;
import com.example.companybackend.entity.RefreshToken;
import com.example.companybackend.repository.DepartmentRepository;
import com.example.companybackend.repository.PositionRepository;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.repository.RefreshTokenRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProviderService tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PositionRepository positionRepository,
                       DepartmentRepository departmentRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProviderService tokenProvider,
                       RefreshTokenRepository refreshTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * ユーザー認証とトークン生成
     */
    public String authenticateUser(String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            return tokenProvider.generateToken(authentication);
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
        user.setManagerId(null);
        user.setDepartmentId(null);
        user.setPositionId(null);
        
        return userRepository.save(user);
    }

    /**
     * 管理者によるユーザー登録
     */
    public User registerUserByAdmin(User user, String adminUsername) {
        // 管理者権限チェック
        User adminUser = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("管理者ユーザーが見つかりません"));
        
        // 管理者レベルチェック（level >= 5）
        Position adminPosition = positionRepository.findById(adminUser.getPositionId())
                .orElseThrow(() -> new RuntimeException("管理者の役職が見つかりません"));
        
        if (adminPosition.getLevel() < 5) {
            throw new RuntimeException("権限が不足しています。管理者ユーザーのみ登録可能です。");
        }

        // ユーザー名重複チェック
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("ユーザー名は既に使用されています");
        }

        // 役職の存在チェック
        if (user.getPositionId() != null && !positionRepository.existsById(user.getPositionId())) {
            throw new RuntimeException("指定された役職は存在しません");
        }

        // 部署の存在チェック
        if (user.getDepartmentId() != null && !departmentRepository.existsById(user.getDepartmentId())) {
            throw new RuntimeException("指定された部署は存在しません");
        }

        // パスワードハッシュ化
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        
        // 作成日時と更新日時を設定
        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        
        return userRepository.save(user);
    }

    /**
     * IDによるユーザー取得（Optionalを返すバージョン）
     */
    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * ユーザーIDからユーザーを取得（例外をスローするバージョン）
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
    }
    
    /**
     * ユーザー名からユーザー情報を取得
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
    }

    /**
     * ユーザー名存在チェック
     */
    public boolean checkUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * ユーザー名からアクセストークンを生成
     */
    public String generateToken(String username) {
        return tokenProvider.generateToken(username);
    }

    /**
     * 部署IDから部署名を取得
     */
    public String getDepartmentNameById(Integer departmentId) {
        if (departmentId == null) {
            return null;
        }
        
        return departmentRepository.findById(departmentId)
                .map(Department::getName)
                .orElse(null);
    }

    /**
     * 役職IDから役職名を取得
     */
    public String getPositionNameById(Integer positionId) {
        if (positionId == null) {
            return null;
        }
        
        return positionRepository.findById(positionId)
                .map(Position::getName)
                .orElse(null);
    }

    /**
     * 管理者役職一覧取得
     */
    public List<AdminPositionsResponse.PositionData> getAdminPositions() {
        // level >= 5 の役職を取得
        List<Position> positions = positionRepository.findByLevelGreaterThanEqualOrderByLevelDesc(5);
        
        return positions.stream()
                .map(position -> new AdminPositionsResponse.PositionData(
                        position.getId(),
                        position.getName(),
                        position.getLevel()))
                .collect(Collectors.toList());
    }

    /**
     * CSVからのユーザー一括登録
     */
    public int[] registerUsersFromCsv(List<CsvUserData> csvUsers) {
        int successCount = 0;
        int errorCount = 0;
        
        for (CsvUserData csvUser : csvUsers) {
            try {
                User user = new User();
                user.setUsername(csvUser.getUsername());
                user.setPasswordHash(passwordEncoder.encode(csvUser.getPassword()));
                user.setFullName(csvUser.getFullName());
                user.setLocationType(csvUser.getLocationType());
                user.setClientLatitude(csvUser.getClientLatitude());
                user.setClientLongitude(csvUser.getClientLongitude());
                user.setDepartmentId(csvUser.getDepartmentId());
                user.setPositionId(csvUser.getPositionId());
                user.setManagerId(csvUser.getManagerId());
                
                // 作成日時と更新日時を設定
                OffsetDateTime now = OffsetDateTime.now();
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                
                // ユーザー名重複チェック
                if (userRepository.existsByUsername(user.getUsername())) {
                    errorCount++;
                    continue;
                }
                
                userRepository.save(user);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                // エラーがあっても他のユーザーの登録を続行
            }
        }
        
        return new int[]{successCount, errorCount};
    }

    /**
     * CSVファイルからのユーザー一括登録（既存メソッドの互換性維持）
     */
    public String registerUsersFromCsv(MultipartFile file) {
        try {
            List<CsvUserData> csvUsers = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue; // ヘッダー行をスキップ
                    }
                    
                    String[] values = line.split(",");
                    if (values.length >= 3) {
                        CsvUserData user = new CsvUserData();
                        user.setUsername(values[0]);
                        user.setPassword(values[1]);
                        user.setFullName(values[2]);
                        // 他のフィールドも必要に応じて設定
                        csvUsers.add(user);
                    }
                }
            }
            
            int[] result = registerUsersFromCsv(csvUsers);
            return String.format("登録成功: %d件, 登録失敗: %d件", result[0], result[1]);
            
        } catch (Exception e) {
            throw new RuntimeException("CSVファイルの処理中にエラーが発生しました: " + e.getMessage());
        }
    }
    
    /**
     * トークンの検証
     */
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }
    
    /**
     * リフレッシュトークンの作成
     */
    public RefreshToken createRefreshToken(User user) {
        // 7日間有効なリフレッシュトークンを生成
        OffsetDateTime expiryDate = OffsetDateTime.now().plus(7, ChronoUnit.DAYS);
        
        // JWTトークンを生成
        String token = tokenProvider.generateRefreshToken(user.getUsername());
        
        // リフレッシュトークンエンティティを作成
        RefreshToken refreshToken = new RefreshToken(token, user.getId(), expiryDate);
        
        // 既存のリフレッシュトークンを削除
        refreshTokenRepository.deleteByUserId(user.getId());
        
        // 新しいリフレッシュトークンを保存
        return refreshTokenRepository.save(refreshToken);
    }
    
    /**
     * リフレッシュトークンの検証
     */
    public RefreshToken validateRefreshToken(String token) {
        // トークンの形式を検証
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("リフレッシュトークンが無効です");
        }
        
        // データベースからトークンを検索
        RefreshToken refreshToken = refreshTokenRepository.findValidByToken(token)
                .orElseThrow(() -> new RuntimeException("リフレッシュトークンが無効です"));
        
        // トークンが有効か確認
        if (!refreshToken.isValid()) {
            throw new RuntimeException("リフレッシュトークンが無効です");
        }
        
        return refreshToken;
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
    
    /**
     * ユーザー認証とトークン・リフレッシュトークン生成
     */
    public AuthResult authenticateUserWithTokens(String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // アクセストークンを生成
            String accessToken = tokenProvider.generateToken(authentication);
            
            // ユーザー情報を取得
            User user = getUserByUsername(username);
            
            // リフレッシュトークンを生成
            RefreshToken refreshTokenEntity = createRefreshToken(user);
            String refreshToken = refreshTokenEntity.getToken();
            
            return new AuthResult(accessToken, refreshToken, user);
        } catch (Exception e) {
            throw new RuntimeException("認証に失敗しました: " + e.getMessage());
        }
    }
    
    public static class AuthResult {
        private final String accessToken;
        private final String refreshToken;
        private final User user;
        
        public AuthResult(String accessToken, String refreshToken, User user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }
        
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public User getUser() { return user; }
    }
}