package com.example.companybackend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS防护请求包装器
 * 用于拦截HTTP请求并对参数进行清理以防止XSS攻击
 */
public class XssProtectionRequestWrapper extends HttpServletRequestWrapper {
    
    private final HtmlSanitizerService sanitizerService;
    
    public XssProtectionRequestWrapper(HttpServletRequest request, HtmlSanitizerService sanitizerService) {
        super(request);
        this.sanitizerService = sanitizerService;
    }
    
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if (value != null) {
            // 对于特定字段进行HTML清理，其他字段进行HTML转义
            if (isHtmlContentField(name)) {
                return sanitizerService.sanitizeHtml(value);
            } else if (!isSecurityField(name)) {
                // 不对安全相关字段（如CSRF令牌）进行转义
                return sanitizerService.escapeHtml(value);
            }
        }
        return value;
    }
    
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> originalMap = super.getParameterMap();
        Map<String, String[]> cleanedMap = new HashMap<>();
        
        for (Map.Entry<String, String[]> entry : originalMap.entrySet()) {
            String key = entry.getKey();
            String[] originalValues = entry.getValue();
            String[] cleanedValues = new String[originalValues.length];
            
            for (int i = 0; i < originalValues.length; i++) {
                if (originalValues[i] != null) {
                    if (isHtmlContentField(key)) {
                        cleanedValues[i] = sanitizerService.sanitizeHtml(originalValues[i]);
                    } else if (!isSecurityField(key)) {
                        // 不对安全相关字段（如CSRF令牌）进行转义
                        cleanedValues[i] = sanitizerService.escapeHtml(originalValues[i]);
                    } else {
                        // 安全字段保持原样
                        cleanedValues[i] = originalValues[i];
                    }
                }
            }
            
            cleanedMap.put(key, cleanedValues);
        }
        
        return Collections.unmodifiableMap(cleanedMap);
    }
    
    @Override
    public String[] getParameterValues(String name) {
        String[] originalValues = super.getParameterValues(name);
        if (originalValues == null) {
            return null;
        }
        
        String[] cleanedValues = new String[originalValues.length];
        for (int i = 0; i < originalValues.length; i++) {
            if (originalValues[i] != null) {
                if (isHtmlContentField(name)) {
                    cleanedValues[i] = sanitizerService.sanitizeHtml(originalValues[i]);
                } else if (!isSecurityField(name)) {
                    // 不对安全相关字段（如CSRF令牌）进行转义
                    cleanedValues[i] = sanitizerService.escapeHtml(originalValues[i]);
                } else {
                    // 安全字段保持原样
                    cleanedValues[i] = originalValues[i];
                }
            }
        }
        
        return cleanedValues;
    }
    
    /**
     * 判断字段是否为HTML内容字段
     * @param fieldName 字段名
     * @return 是否为HTML内容字段
     */
    private boolean isHtmlContentField(String fieldName) {
        // 只对明确需要HTML内容处理的字段进行清理
        return ("bio".equalsIgnoreCase(fieldName) || 
               "description".equalsIgnoreCase(fieldName) ||
               "content".equalsIgnoreCase(fieldName));
    }
    
    /**
     * 判断字段是否为安全相关字段
     * @param fieldName 字段名
     * @return 是否为安全相关字段
     */
    private boolean isSecurityField(String fieldName) {
        // 不对安全相关字段进行转义，避免干扰安全机制
        return "X-CSRF-TOKEN".equalsIgnoreCase(fieldName) ||
               "csrfToken".equalsIgnoreCase(fieldName) ||
               fieldName.toLowerCase().contains("token") ||
               fieldName.toLowerCase().contains("csrf");
    }
}