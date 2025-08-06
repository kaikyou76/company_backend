package com.example.companybackend.security;

import com.example.companybackend.entity.User;
import com.example.companybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

/**
 * カスタムUserDetailsService
 * comsys_dump.sql usersテーブル準拠
 * 24名テストユーザー対応
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepository;

    /**
     * ユーザー名でユーザー詳細を読み込み
     * 
     * @param username ユーザー名（email）
     * @return UserDetails
     * @throws UsernameNotFoundException ユーザーが見つからない場合
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        log.debug("User found: {} (ID: {})", user.getUsername(), user.getId());
        return createUserPrincipal(user);
    }

    /**
     * ユーザーIDでユーザー詳細を読み込み
     * 
     * @param userId ユーザーID
     * @return UserDetails
     * @throws UsernameNotFoundException ユーザーが見つからない場合
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UsernameNotFoundException("User not found with ID: " + userId);
                });

        log.debug("User found: {} (ID: {})", user.getUsername(), user.getId());
        return createUserPrincipal(user);
    }

    /**
     * UserPrincipal作成
     * 
     * @param user ユーザーエンティティ
     * @return UserDetails
     */
    private UserDetails createUserPrincipal(User user) {
        Collection<GrantedAuthority> authorities = getAuthorities(user);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * ユーザー権限取得
     * 
     * @param user ユーザーエンティティ
     * @return 権限リスト
     */
    private Collection<GrantedAuthority> getAuthorities(User user) {
        // 基本的にはUSERロールを付与
        // 将来的には役職や部署に基づいて権限を決定
        if (user.isManager()) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_MANAGER"));
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}