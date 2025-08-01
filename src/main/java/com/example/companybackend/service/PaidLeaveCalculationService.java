package com.example.companybackend.service;

import com.example.companybackend.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class PaidLeaveCalculationService {

    /**
     * ユーザーの勤続年数に基づいて有給休暇日数を計算する
     * 
     * @param user ユーザー情報
     * @param targetDate 基準日
     * @return 有給休暇日数
     */
    public int calculatePaidLeaveDays(User user, LocalDate targetDate) {
        log.info("有給休暇日数計算: userId={}, targetDate={}", user.getId(), targetDate);
        
        // ユーザーの作成日（入社日と仮定）を基準に勤続年数を計算
        LocalDate hireDate = user.getCreatedAt().toLocalDate();
        long yearsOfService = ChronoUnit.YEARS.between(hireDate, targetDate);
        
        log.debug("勤続年数計算結果: userId={}, hireDate={}, yearsOfService={}", 
                  user.getId(), hireDate, yearsOfService);
        
        // 勤続年数に基づく有給休暇日数
        int paidLeaveDays = calculateByYearsOfService(yearsOfService);
        
        log.info("有給休暇日数計算完了: userId={}, paidLeaveDays={}", user.getId(), paidLeaveDays);
        return paidLeaveDays;
    }
    
    /**
     * 勤続年数に基づいて有給休暇日数を決定する
     * 
     * @param yearsOfService 勤続年数
     * @return 有給休暇日数
     */
    private int calculateByYearsOfService(long yearsOfService) {
        // 日本の労働基準法に基づく有給休暇日数
        if (yearsOfService < 1) {
            // 6か月以上1年未満の場合、10日
            return yearsOfService >= 0.5 ? 10 : 0;
        } else if (yearsOfService < 2) {
            return 10;  // 1年以上2年未満の場合、10日
        } else if (yearsOfService < 3) {
            return 11;  // 2年以上3年未満の場合、11日
        } else if (yearsOfService < 4) {
            return 12;  // 3年以上4年未満の場合、12日
        } else if (yearsOfService < 5) {
            return 13;  // 4年以上5年未満の場合、13日
        } else if (yearsOfService < 6) {
            return 14;  // 5年以上6年未満の場合、14日
        } else {
            return 15;  // 6年以上の場合、15日（上限）
        }
    }
}