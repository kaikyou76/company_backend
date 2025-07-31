package com.example.companybackend.util;

import com.example.companybackend.dto.auth.CsvUserData;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV解析ユーティリティクラス
 */
public class CsvParsingUtil {
    
    /**
     * CSVファイルを解析してユーザーデータリストに変換
     * 
     * @param file CSVファイル
     * @return ユーザーデータリスト
     * @throws IOException ファイル読み込みエラー
     */
    public static List<CsvUserData> parseCsvFile(MultipartFile file) throws IOException {
        List<CsvUserData> users = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            boolean isFirstLine = true;
            String[] headers = null;
            
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",", -1);
                
                // 最初の行をヘッダーとして処理
                if (isFirstLine) {
                    headers = values;
                    isFirstLine = false;
                    continue;
                }
                
                // データ行を処理
                CsvUserData user = parseUserDataFromCsvRow(headers, values);
                if (user != null) {
                    users.add(user);
                }
            }
        }
        
        return users;
    }
    
    /**
     * CSV行データからユーザーデータを解析
     * 
     * @param headers ヘッダー配列
     * @param values 値配列
     * @return ユーザーデータ
     */
    private static CsvUserData parseUserDataFromCsvRow(String[] headers, String[] values) {
        if (values.length != headers.length) {
            return null; // ヘッダーと値の数が一致しない場合はスキップ
        }
        
        CsvUserData user = new CsvUserData();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim().toLowerCase();
            String value = values[i].trim();
            
            if (value.isEmpty()) {
                continue; // 空の値はスキップ
            }
            
            switch (header) {
                case "username":
                    user.setUsername(value);
                    break;
                case "password":
                    user.setPassword(value);
                    break;
                case "fullname":
                    user.setFullName(value);
                    break;
                case "location_type":
                    user.setLocationType(value.toLowerCase());
                    break;
                case "client_latitude":
                    try {
                        user.setClientLatitude(Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        // 数値に変換できない場合はスキップ
                    }
                    break;
                case "client_longitude":
                    try {
                        user.setClientLongitude(Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        // 数値に変換できない場合はスキップ
                    }
                    break;
                case "department_id":
                    try {
                        user.setDepartmentId(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        // 数値に変換できない場合はスキップ
                    }
                    break;
                case "position_id":
                    try {
                        user.setPositionId(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        // 数値に変換できない場合はスキップ
                    }
                    break;
                case "manager_id":
                    try {
                        user.setManagerId(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        // 数値に変換できない場合はスキップ
                    }
                    break;
            }
        }
        
        // 必須フィールドが設定されているか確認
        if (user.getUsername() != null && user.getPassword() != null && user.getFullName() != null) {
            return user;
        }
        
        return null; // 必須フィールドが不足している場合はnullを返す
    }
}