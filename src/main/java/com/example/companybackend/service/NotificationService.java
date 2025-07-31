package com.example.companybackend.service;

import com.example.companybackend.entity.LeaveRequest;
import com.example.companybackend.entity.User;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    /**
     * 休暇申請通知送信
     * @param leaveRequest 休暇申請
     * @param user 申請者
     */
    public void sendLeaveRequestNotification(LeaveRequest leaveRequest, User user) {
        // 仮の実装 - 実際にはメールやSlack等で通知を送信する
        System.out.println("通知送信: " + user.getUsername() + " に休暇申請の通知を送信しました。");
    }
}