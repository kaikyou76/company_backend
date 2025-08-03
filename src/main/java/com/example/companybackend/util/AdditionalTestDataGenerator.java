package com.example.companybackend.util;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 追加テストデータ生成ユーティリティ
 * 
 * このクラスは、バッチ処理のテスト用に追加の模擬データを生成します。
 * 生成されるデータ:
 * 1. 休日データ (holidays)
 * 2. 勤務場所データ (work_locations)
 */
public class AdditionalTestDataGenerator {
    
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        try {
            System.out.println("追加テストデータの生成を開始します...");
            generateHolidays();
            generateWorkLocations();
            System.out.println("追加テストデータの生成が完了しました。");
            System.out.println("生成されたファイル:");
            System.out.println("1. holidays_data.sql - 休日データ");
            System.out.println("2. work_locations_data.sql - 勤務場所データ");
        } catch (Exception e) {
            System.err.println("追加テストデータの生成中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 休日データを生成する
     */
    private static void generateHolidays() throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        try (FileWriter writer = new FileWriter("holidays_data.sql")) {
            writer.write("-- 休日テストデータ\n");
            
            List<Holiday> holidays = new ArrayList<>();
            
            // 固定的祝日を追加
            holidays.add(new Holiday(LocalDate.of(2025, 1, 1), "元日", true));
            holidays.add(new Holiday(LocalDate.of(2025, 1, 13), "成人の日", false));
            holidays.add(new Holiday(LocalDate.of(2025, 2, 11), "建国記念の日", true));
            holidays.add(new Holiday(LocalDate.of(2025, 4, 29), "昭和の日", true));
            holidays.add(new Holiday(LocalDate.of(2025, 5, 3), "憲法記念日", true));
            holidays.add(new Holiday(LocalDate.of(2025, 5, 4), "みどりの日", true));
            holidays.add(new Holiday(LocalDate.of(2025, 5, 5), "こどもの日", true));
            holidays.add(new Holiday(LocalDate.of(2025, 7, 21), "海の日", false));
            holidays.add(new Holiday(LocalDate.of(2025, 8, 11), "山の日", true));
            holidays.add(new Holiday(LocalDate.of(2025, 9, 15), "敬老の日", false));
            holidays.add(new Holiday(LocalDate.of(2025, 9, 23), "秋分の日", true));
            holidays.add(new Holiday(LocalDate.of(2025, 10, 13), "体育の日", false));
            holidays.add(new Holiday(LocalDate.of(2025, 11, 3), "文化の日", true));
            holidays.add(new Holiday(LocalDate.of(2025, 11, 23), "勤労感謝の日", true));
            holidays.add(new Holiday(LocalDate.of(2025, 12, 23), "天皇誕生日", true));
            
            // 過去の特定の休日を追加
            holidays.add(new Holiday(LocalDate.of(2024, 12, 31), "年末休暇", false));
            holidays.add(new Holiday(LocalDate.of(2025, 12, 31), "年末休暇", false));
            
            // テスト用にランダムな休日をいくつか追加
            for (int i = 1; i <= 10; i++) {
                int year = 2025;
                int month = random.nextInt(12) + 1;
                int day = random.nextInt(28) + 1; // 簡略化のため常に有効な日付を使用
                
                LocalDate date = LocalDate.of(year, month, day);
                String name = "テスト休日" + i;
                boolean isRecurring = random.nextBoolean();
                
                holidays.add(new Holiday(date, name, isRecurring));
            }
            
            // SQLファイルに書き込む
            for (Holiday holiday : holidays) {
                String createdAt = LocalDate.now().format(formatter) + " 00:00:00";
                writer.write(String.format("%s\t%s\t%s\t%s 00:00:00\n",
                    holiday.date.format(formatter), 
                    holiday.name, 
                    holiday.isRecurring ? "t" : "f",
                    createdAt));
            }
        }
        
        System.out.println("休日データを生成しました");
    }
    
    /**
     * 勤務場所データを生成する
     */
    private static void generateWorkLocations() throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        try (FileWriter writer = new FileWriter("work_locations_data.sql")) {
            writer.write("-- 勤務場所テストデータ\n");
            
            String createdAt = LocalDate.now().format(formatter) + " 00:00:00";
            
            // 勤務場所データ
            writer.write(String.format("%s\t%s\t%.4f\t%.4f\t%d\t%s\t%s\n",
                "本社オフィス", "office", 35.6812, 139.7671, 100, "t", createdAt));
            
            writer.write(String.format("%s\t%s\t%.4f\t%.4f\t%d\t%s\t%s\n",
                "新宿支社", "office", 35.6896, 139.7006, 80, "t", createdAt));
            
            writer.write(String.format("%s\t%s\t%.4f\t%.4f\t%d\t%s\t%s\n",
                "大阪支社", "office", 34.7024, 135.4959, 120, "t", createdAt));
            
            writer.write(String.format("%s\t%s\t%.4f\t%.4f\t%d\t%s\t%s\n",
                "福岡支社", "office", 33.5904, 130.4017, 100, "t", createdAt));
            
            writer.write(String.format("%s\t%s\t%.4f\t%.4f\t%d\t%s\t%s\n",
                "クライアントA", "client", 35.6762, 139.6503, 100, "t", createdAt));
            
            writer.write(String.format("%s\t%s\t%.4f\t%.4f\t%d\t%s\t%s\n",
                "クライアントB", "client", 35.6399, 139.7224, 100, "t", createdAt));
            
            writer.write(String.format("%s\t%s\t%.4f\t%.4f\t%d\t%s\t%s\n",
                "クライアントC", "client", 35.6329, 139.8821, 100, "t", createdAt));
            
            writer.write(String.format("%s\t%s\t%.4f\t%.4f\t%d\t%s\t%s\n",
                "クライアントD", "client", 35.7295, 139.7123, 100, "t", createdAt));
        }
        
        System.out.println("勤務場所データを生成しました");
    }
    
    /**
     * 休日情報を保持するための内部クラス
     */
    private static class Holiday {
        LocalDate date;
        String name;
        boolean isRecurring;
        
        public Holiday(LocalDate date, String name, boolean isRecurring) {
            this.date = date;
            this.name = name;
            this.isRecurring = isRecurring;
        }
    }
}