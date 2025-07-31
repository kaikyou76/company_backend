package com.example.companybackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 登録リクエストDTO
 * comsys_dump.sql usersテーブル準拠
 */
/**
 * 登録リクエストDTO
 * comsys_dump.sql usersテーブル準拠
 */
@Data
public class RegisterRequest {
    
    @NotBlank(message = "ユーザー名は必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String username;
    
    @NotBlank(message = "パスワードは必須です")
    private String password;
    
    @NotBlank(message = "パスワード確認は必須です")
    private String confirmPassword;
    
    @NotBlank(message = "氏名は必須です")
    private String fullName;
    
    @NotBlank(message = "勤務地タイプは必須です")
    @Pattern(regexp = "office|client", message = "勤務地タイプは'office'または'client'である必要があります")
    private String locationType;
    
    private Double clientLatitude;
    private Double clientLongitude;
    private Integer managerId;
    private Integer departmentId;
    private Integer positionId;
}