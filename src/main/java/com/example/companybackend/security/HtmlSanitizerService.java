package com.example.companybackend.security;

import org.apache.commons.text.StringEscapeUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

/**
 * HTML清理服务，用于防止XSS攻击
 * 提供HTML转义和清理功能
 */
@Service
public class HtmlSanitizerService {
    
    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
        .allowElements("a", "b", "br", "em", "i", "li", "ol", "p", "span", "strong", "ul", "div", "h1", "h2", "h3", "h4", "h5", "h6")
        .allowAttributes("href").onElements("a")
        .allowAttributes("class").onElements("div", "span", "p", "h1", "h2", "h3", "h4", "h5", "h6")
        .allowStandardUrlProtocols()
        .toFactory();
    
    /**
     * 对HTML内容进行清理，防止XSS攻击
     * 使用OWASP HTML Sanitizer进行专业的HTML清理
     * @param htmlInput 用户输入的可能包含HTML的内容
     * @return 清理后的内容
     */
    public String sanitizeHtml(String htmlInput) {
        if (htmlInput == null || htmlInput.isEmpty()) {
            return htmlInput;
        }
        // 使用OWASP HTML Sanitizer进行专业清理
        return POLICY.sanitize(htmlInput);
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
        return StringEscapeUtils.escapeHtml4(textInput);
    }
}