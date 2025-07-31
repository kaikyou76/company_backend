package com.example.companybackend.service;

import com.example.companybackend.entity.User;
import com.example.companybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService実装
 * comsys_dump.sql usersテーブル対応
 * 24名テストユーザー認証
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("ユーザー認証処理開始: {}", username);
        
        User user = userRepository.findByUsernameForAuthentication(username)
                .orElseThrow(() -> {
                    log.warn("ユーザーが見つかりません: {}", username);
                    return new UsernameNotFoundException("ユーザーが見つかりません: " + username);
                });

        log.debug("ユーザー認証処理完了: {} (ID: {}, 勤務地: {})", 
                username, user.getId(), user.getLocationType());
        
        return user;
    }
}