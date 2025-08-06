package com.example.companybackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * XSS防护过滤器
 * 用于拦截HTTP请求并应用XSS防护机制
 */
@Component
@Order(100) // 设置较低优先级，确保在CSRF过滤器之后执行
public class XssProtectionFilter extends OncePerRequestFilter {
    
    private final HtmlSanitizerService sanitizerService;
    
    public XssProtectionFilter(HtmlSanitizerService sanitizerService) {
        this.sanitizerService = sanitizerService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        // 包装请求以实现参数清理
        XssProtectionRequestWrapper wrappedRequest = new XssProtectionRequestWrapper(request, sanitizerService);
        filterChain.doFilter(wrappedRequest, response);
    }
}