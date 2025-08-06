package com.example.companybackend.security;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;

/**
 * HTML清理服务，用于防止XSS攻击
 * 提供HTML转义和清理功能
 */
@Service
public class HtmlSanitizerService {
    
    /**
     * 对HTML内容进行转义，防止XSS攻击
     * @param htmlInput 用户输入的可能包含HTML的内容
     * @return 转义后的内容
     */
    public String sanitizeHtml(String htmlInput) {
        if (htmlInput == null || htmlInput.isEmpty()) {
            return htmlInput;
        }
        // 使用Apache Commons Text进行HTML转义
        String escaped = StringEscapeUtils.escapeHtml4(htmlInput);
        // 额外处理JavaScript代码中的潜在XSS攻击
        return sanitizeJavaScriptContent(escaped);
    }
    
    /**
     * 对纯文本内容进行HTML转义
     * @param textInput 用户输入的文本内容
     * @return 转义后的安全文本内容
     */
    public String escapeHtml(String textInput) {
        if (textInput == null || textInput.isEmpty()) {
            return textInput;
        }
        // 使用Apache Commons Text进行HTML转义
        String escaped = StringEscapeUtils.escapeHtml4(textInput);
        // 额外处理JavaScript代码中的潜在XSS攻击
        return sanitizeJavaScriptContent(escaped);
    }
    
    /**
     * 处理JavaScript代码中的潜在XSS攻击
     * @param input 输入内容
     * @return 处理后的内容
     */
    private String sanitizeJavaScriptContent(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // 移除潜在的JavaScript事件处理器
        String sanitized = input.replaceAll("(?i)\\s*on\\w+\\s*=\\s*['\"].*?['\"]", "");
        
        // 移除script标签内容（即使已经被转义）
        sanitized = sanitized.replaceAll("(?i)&lt;\\s*script\\s*&gt;.*?&lt;\\s*/\\s*script\\s*&gt;", 
                                         "&lt;script&gt;&lt;/script&gt;");
        
        // 移除javascript:协议
        sanitized = sanitized.replaceAll("(?i)javascript\\s*:", "");
        
        // 移除常见的XSS攻击模式，包括alert、eval等
        sanitized = sanitized.replaceAll("(?i)alert\\s*\\(.*?\\)", "")
                             .replaceAll("(?i)eval\\s*\\(.*?\\)", "")
                             .replaceAll("(?i)document\\s*\\.", "")
                             .replaceAll("(?i)console\\s*\\.", "");
        
        return sanitized;
    }
}