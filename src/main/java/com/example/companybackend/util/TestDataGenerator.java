package com.example.companybackend.util;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * テストデータ生成ユーティリティ
 * 
 * このクラスは、バッチ処理のテスト用に大量の模擬データを生成します。
 * 生成されるデータ:
 * 1. 勤怠記録データ (attendance_records)
 * 2. ユーザーデータ (users)
 * 3. 部署データ (departments)
 * 4. 役職データ (positions)
 * 
 * 使用方法:
 * 1. このファイルをコンパイル: javac TestDataGenerator.java
 * 2. 実行: java TestDataGenerator
 * 3. 生成されたSQLファイルをデータベースにインポート
 */
public class TestDataGenerator {
    
    private static final String[] FIRST_NAMES = {
        "太郎", "花子", "一郎", "美咲", "翔太", "愛美", "健太", "彩花", "大輔", "梨奈",
        "優太", "沙織", "直樹", "麻衣", "拓也", "亜紀", "亮介", "恵子", "雄太", "美月",
        "智也", "佳奈", "和也", "由美", "達也", "真理", "充", "里奈", "進", "香織",
        "康平", "菜月", "浩二", "美香", "修平", "舞", "亮太", "美穂", "正志", "亜由美",
        "将也", "恵美", "拓哉", "美紀", "真一", "理奈", "隆之介", "彩夏", "雄介", "美帆"
    };
    
    private static final String[] LAST_NAMES = {
        "佐藤", "鈴木", "高橋", "田中", "渡辺", "伊藤", "山本", "中村", "小林", "加藤",
        "吉田", "山田", "佐々木", "山口", "松本", "井上", "木村", "林", "斎藤", "清水",
        "山崎", "森", "池田", "橋本", "阿部", "石川", "前田", "荒川", "中川", "西村",
        "杉山", "近藤", "坂本", "遠藤", "青木", "藤田", "金子", "後藤", "岡田", "長谷川",
        "村上", "関", "横山", "竹内", "田村", "丹羽", "白井", "原田", "小野", "岡本"
    };
    
    private static final String[] DEPARTMENTS = {
        "開発部", "営業部", "人事部", "経理部", "総務部", "法務部", "マーケティング部", "品質保証部", "研究開発部", "情報システム部"
    };
    
    private static final String[] DEPARTMENT_CODES = {
        "DEV", "SALES", "HR", "FIN", "GA", "LEGAL", "MKT", "QA", "RND", "IT"
    };
    
    private static final String[] POSITIONS = {
        "一般社員", "主任", "リーダー", "課長", "副課長", "部長", "副部長", "取締役", "CEO"
    };
    
    private static final int[] POSITION_LEVELS = {2, 4, 3, 6, 5, 8, 7, 9, 10};
    
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        try {
            System.out.println("テストデータの生成を開始します...");
            // 使用数据库中已有的用户ID (1-100)
            generateAttendanceRecords(10000, 100);
            System.out.println("テストデータの生成が完了しました。");
            System.out.println("生成されたファイル:");
            System.out.println("1. attendance_records_data.sql - 勤怠記録データ");
            System.out.println("\nデータベースへのインポート方法:");
            System.out.println("psql -U postgres -d company_db -f attendance_records_data.sql");
        } catch (Exception e) {
            System.err.println("テストデータの生成中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 大量の勤怠データを生成する
     * @param count 生成するレコード数（出勤・退勤のペアで1セット）
     * @param maxUserId 最大ユーザーID
     */
    private static void generateAttendanceRecords(int count, int maxUserId) throws IOException {
        // 修复日期格式问题，移除时区信息
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime baseDate = LocalDateTime.now().minusMonths(3);
        
        try (FileWriter writer = new FileWriter("attendance_records_data.sql")) {
            writer.write("-- 勤怠記録テストデータ\n");
            writer.write("INSERT INTO public.attendance_records (user_id, type, \"timestamp\", latitude, longitude, created_at) VALUES\n");
            
            long id = 69; // 既存のデータに続くID
            for (int i = 0; i < count; i++) {
                // 使用1到maxUserId范围内的用户ID
                long userId = random.nextInt(maxUserId) + 1;
                
                // 通常は出勤・退勤のペアを生成
                LocalDateTime inTime = baseDate.plusDays(random.nextInt(90))
                    .withHour(7 + random.nextInt(3)) // 7-9時
                    .withMinute(random.nextInt(60))
                    .withSecond(random.nextInt(60));
                
                LocalDateTime outTime = inTime.plusHours(8 + random.nextInt(5)) // 8-12時間後
                    .withMinute(random.nextInt(60))
                    .withSecond(random.nextInt(60));
                
                // 緯度経度 (東京付近)
                double latitude = 35.68 + (random.nextDouble() - 0.5) * 0.1;
                double longitude = 139.77 + (random.nextDouble() - 0.5) * 0.1;
                
                // 出勤記録
                writer.write(String.format("(%d, '%s', '%s', %.4f, %.4f, '%s')",
                    userId, "in", inTime.format(formatter), latitude, longitude, 
                    LocalDateTime.now().format(formatter)));
                
                // 退勤記録
                if (i < count - 1) {
                    writer.write(String.format(",\n(%d, '%s', '%s', %.4f, %.4f, '%s')", 
                        userId, "out", outTime.format(formatter), latitude, longitude,
                        LocalDateTime.now().format(formatter)));
                } else {
                    writer.write(String.format(";\n\\.\n"));
                }
                
                // 進行状況の表示
                if ((i + 1) % 1000 == 0) {
                    System.out.println("勤怠記録データ " + (i + 1) + " 件生成完了");
                }
            }
            
            // Remove this line as it's no longer needed with INSERT statements
        }
        
        System.out.println("合計 " + (count * 2) + " 件の勤怠記録データを生成しました");
    }
    
    /**
     * ユーザーデータを生成する
     * @param count 生成するユーザー数
     */
    private static void generateUsers(int count) throws IOException {
        // 修复日期格式问题
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        try (FileWriter writer = new FileWriter("users_data.sql")) {
            writer.write("-- ユーザーテストデータ\n");
            writer.write("COPY public.users (id, username, password_hash, location_type, client_latitude, client_longitude, manager_id, department_id, position_id, created_at, updated_at) FROM stdin;\n");
            
            for (int i = 1; i <= count; i++) {
                String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                String username = String.format("%s.%s@company.com", 
                    lastName.toLowerCase(), firstName.toLowerCase());
                
                String passwordHash = "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6"; // ダミーのパスワードハッシュ
                
                String locationType = random.nextBoolean() ? "office" : "client";
                
                Integer managerId = null;
                if (i > 10) { // 最初の10人は管理者
                    managerId = random.nextInt(10) + 1;
                }
                
                int departmentId = random.nextInt(10) + 1;
                int positionId = random.nextInt(8) + 1; // 1-8 (一般社員から取締役まで)
                
                // 修复日期格式问题
                String createdAt = LocalDateTime.now().format(formatter);
                
                writer.write(String.format("%d\t%s\t%s\t%s\t\\N\t\\N\t%s\t%d\t%d\t%s\t%s\n",
                    i + 24, // 既存の24ユーザーに続く
                    username, passwordHash, locationType, 
                    managerId == null ? "\\N" : managerId.toString(), 
                    departmentId, positionId, createdAt, createdAt));
                
                // 進行状況の表示
                if ((i) % 20 == 0) {
                    System.out.println("ユーザーデータ " + i + " 件生成完了");
                }
            }
            
            writer.write("\\.\n");
        }
        
        System.out.println("合計 " + count + " 件のユーザーデータを生成しました");
    }
    
    /**
     * 部署データを生成する
     */
    private static void generateDepartments() throws IOException {
        // 修复日期格式问题
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        try (FileWriter writer = new FileWriter("departments_data.sql")) {
            writer.write("-- 部署テストデータ\n");
            writer.write("COPY public.departments (id, name, code, manager_id, created_at) FROM stdin;\n");
            
            for (int i = 0; i < DEPARTMENTS.length; i++) {
                // 修复日期格式问题
                String createdAt = LocalDateTime.now().format(formatter);
                writer.write(String.format("%d\t%s\t%s\t\\N\t%s\n", 
                    i + 11, // 既存の10部署に続く
                    DEPARTMENTS[i], DEPARTMENT_CODES[i], createdAt));
            }
            
            writer.write("\\.\n");
        }
        
        System.out.println("部署データを生成しました");
    }
    
    /**
     * 役職データを生成する
     */
    private static void generatePositions() throws IOException {
        // 修复日期格式问题
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        try (FileWriter writer = new FileWriter("positions_data.sql")) {
            writer.write("-- 役職テストデータ\n");
            writer.write("COPY public.positions (id, name, level, created_at) FROM stdin;\n");
            
            for (int i = 0; i < POSITIONS.length; i++) {
                // 修复日期格式问题
                String createdAt = LocalDateTime.now().format(formatter);
                writer.write(String.format("%d\t%s\t%d\t%s\n", 
                    i + 12, // 既存の11役職に続く
                    POSITIONS[i], POSITION_LEVELS[i], createdAt));
            }
            
            writer.write("\\.\n");
        }
        
        System.out.println("役職データを生成しました");
    }
}