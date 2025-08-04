package com.example.companybackend.service;

import com.example.companybackend.entity.Notification;
import com.example.companybackend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Value("${app.notification.email.enabled:false}")
    private boolean emailNotificationEnabled;
    
    /**
     * 发送邮件通知
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param message 邮件内容
     * @param userId 用户ID（可选）
     */
    public void sendEmailNotification(String to, String subject, String message, Long userId) {
        // 检查邮件通知是否启用
        if (!emailNotificationEnabled) {
            logger.info("邮件通知功能未启用，跳过发送邮件: to={}, subject={}", to, subject);
            // 仍然保存通知记录
            saveNotificationRecord(userId, subject, message);
            return;
        }
        
        // 发送邮件
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        
        try {
            mailSender.send(mailMessage);
            logger.info("邮件通知发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            // 记录错误日志，但不中断主流程
            logger.error("邮件通知发送失败: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
            // 注意：这里不抛出异常，以免影响主业务流程
        }
        
        // 保存通知记录（无论邮件是否发送成功）
        saveNotificationRecord(userId, subject, message);
    }
    
    /**
     * 保存通知记录
     * @param userId 用户ID
     * @param subject 主题
     * @param message 消息内容
     */
    private void saveNotificationRecord(Long userId, String subject, String message) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId != null ? Math.toIntExact(userId) : null);
            notification.setTitle(subject);
            notification.setMessage(message);
            notification.setType("system");
            notification.setIsRead(false);
            
            notificationRepository.save(notification);
            logger.info("邮件通知记录保存成功: userId={}, subject={}", userId, subject);
        } catch (Exception e) {
            logger.error("邮件通知记录保存失败: userId={}, subject={}, error={}", userId, subject, e.getMessage(), e);
        }
    }
}